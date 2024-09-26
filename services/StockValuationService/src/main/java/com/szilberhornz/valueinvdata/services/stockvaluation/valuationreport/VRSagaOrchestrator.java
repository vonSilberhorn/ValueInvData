package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.RecordHolder;
import com.szilberhornz.valueinvdata.services.stockvaluation.cache.TickerCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationServerCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.HttpStatusCode;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.FMPResponseHandler;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.RateLimitReachedException;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr.ApiKeyException;
import com.szilberhornz.valueinvdata.services.stockvaluation.persistence.api.ValuationDBRepository;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.circuitbreaker.VRSagaCircuitBreaker;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.formatter.ValuationResponseBodyFormatter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class VRSagaOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(VRSagaOrchestrator.class);

    private static final String INVALID_TICKER_MESSAGE = "The server only responds to valuation report requests " +
            "for real tickers! The ticker %s is not a valid ticker! Please try again with a valid ticker!";

    private final ValuationDBRepository valuationDbRepository;
    private final ValuationServerCache valuationServerCache;
    private final FMPResponseHandler fmpResponseHandler;
    private final TickerCache tickerCache;
    private final ValuationResponseBodyFormatter formatter;
    private final VRSagaCircuitBreaker circuitBreaker;
    private final VRSagaDataBroker dataBroker;

    public VRSagaOrchestrator(final ValuationDBRepository valuationDbRepository, final ValuationServerCache valuationServerCache,
                              final FMPResponseHandler fmpResponseHandler, final TickerCache tickerCache, final ValuationResponseBodyFormatter formatter,
                              final VRSagaCircuitBreaker circuitBreaker, final VRSagaDataBroker dataBroker) {
        this.valuationDbRepository = valuationDbRepository;
        this.valuationServerCache = valuationServerCache;
        this.fmpResponseHandler = fmpResponseHandler;
        this.tickerCache = tickerCache;
        this.formatter = formatter; // must use this with http 200
        this.circuitBreaker = circuitBreaker;
        this.dataBroker = dataBroker;
    }

    private ValuationResponse generateValueReport(final String upperCaseTicker) {
        if (!this.tickerCache.tickerExists(upperCaseTicker)){
            LOG.warn("Received illegal request for invalid ticker {}! Sending back http 403", upperCaseTicker);
            return this.respondToInvalidTicker(upperCaseTicker);
        }
        //try to get report RecordHolder from cache, then from db then from FMP API
        final RecordHolder recordFromCache = this.valuationServerCache.get(upperCaseTicker);
        RecordHolder recordFromDb = null;
        RecordHolder recordFromFmpApi = null;
        if (recordFromCache == null || recordFromCache.isDataMissing()) {
            try {
                //this fills up missing data if it can
                final CompletableFuture<RecordHolder> rhFuture = CompletableFuture.supplyAsync(()-> this.getDataFromDb(recordFromCache, upperCaseTicker));
                //wait till timeout or success, we go to the FMP Api only if we are still missing data
                recordFromDb = rhFuture.get(this.circuitBreaker.getTimeoutForDbQueryInMillis(), TimeUnit.MILLISECONDS);
            } catch (final IllegalStateException illegalStateException){
                //we must break here since we should not return data that we know is inconsistent or otherwise wrong
                return this.handleDbError(upperCaseTicker, illegalStateException);
            } catch (final ExecutionException executionException) {
                //log and move on, the expected IllegalStateException is already handled
                LOG.error("Unexpected exception happened while trying to get data for ticker {} from the database!", upperCaseTicker, executionException.getCause());
            } catch (final InterruptedException interruptedException){
                LOG.error("Unexpected thread interruption while trying to get data for ticker {} from the database!", upperCaseTicker, interruptedException);
            } catch (final TimeoutException timeoutException) {
                //log but otherwise do nothing, we go to the FMP Api
                LOG.error("Circuit breaker timeout reached while trying to get data for ticker {} from the database!", upperCaseTicker);
            }
        } else {
            return new ValuationResponse.Builder()
                    .recordHolder(recordFromCache)
                    .responseBodyFormatter(this.formatter)
                    .statusCode(HttpStatusCode.OK.getStatusCode())
                    .build();
        }
        ValuationResponse finalResponse;
        if (recordFromDb == null || recordFromDb.isDataMissing()){
            try {
                //this is the most data we are going to have
                recordFromFmpApi = this.getDataFromFmpApi(recordFromDb, upperCaseTicker, this.circuitBreaker.getTimeoutForApiCallInMillis());
                finalResponse = new ValuationResponse.Builder()
                        .statusCode(HttpStatusCode.OK.getStatusCode())
                        .recordHolder(recordFromFmpApi)
                        .responseBodyFormatter(this.formatter)
                        .build();
            } catch (final Throwable throwable) {
                //we handle error and return what we can (that is what we have from the db which is equals or a superset of what we have from the cache)
                finalResponse = this.handleFmpApiError(recordFromDb, throwable);
            }
        } else {
            finalResponse = new ValuationResponse.Builder()
                    .recordHolder(recordFromDb)
                    .responseBodyFormatter(this.formatter)
                    .statusCode(HttpStatusCode.OK.getStatusCode())
                    .build();
        }
        //last step is an async call for persisting all the data if needed
        final RecordHolder finalRecordFromDb = recordFromDb;
        final RecordHolder finalRecordFromFmpApi = recordFromFmpApi;
        CompletableFuture.runAsync(() -> this.dataBroker.persistData(recordFromCache, finalRecordFromDb, finalRecordFromFmpApi));
        //we won't wait for the CompletableFuture to finish
        return finalResponse;
    }

    private ValuationResponse respondToInvalidTicker(final String ticker) {
        final String errorMessage = String.format(INVALID_TICKER_MESSAGE, ticker);
        return new ValuationResponse.Builder()
                .statusCode(HttpStatusCode.FORBIDDEN.getStatusCode())
                .errorMessage(errorMessage)
                .build();
    }

    @Nullable
    private RecordHolder getDataFromDb(@Nullable final RecordHolder recordFromCache, final String ticker){
        if (recordFromCache == null){
            return this.valuationDbRepository.queryRecords(ticker);
        } else if (recordFromCache.isDataMissing()){
            DiscountedCashFlowDTO dcfDto = recordFromCache.getDiscountedCashFlowDto();
            PriceTargetSummaryDTO ptsDto = recordFromCache.getPriceTargetSummaryDto();
            PriceTargetConsensusDTO ptcDto = recordFromCache.getPriceTargetConsensusDto();
            if (dcfDto == null){
                dcfDto = this.valuationDbRepository.queryDiscountedCashFlowData(ticker);
            }
            if (ptsDto == null) {
                ptsDto = this.valuationDbRepository.queryPriceTargetSummaryData(ticker);
            }
            if (ptcDto == null){
                ptcDto = this.valuationDbRepository.queryPriceTargetConsensusData(ticker);
            }
            return RecordHolder.newRecordHolder(ticker, dcfDto, ptcDto, ptsDto);
        }
        return recordFromCache;
    }

    @NotNull
    private RecordHolder getDataFromFmpApi(@Nullable final RecordHolder recordFromDb, final String ticker, final long timeOutInSeconds) throws Throwable {
        DiscountedCashFlowDTO dcfDto = null;
        PriceTargetSummaryDTO ptsDto = null;
        PriceTargetConsensusDTO ptcDto = null;
        if (recordFromDb != null){
            dcfDto = recordFromDb.getDiscountedCashFlowDto();
            ptsDto = recordFromDb.getPriceTargetSummaryDto();
            ptcDto = recordFromDb.getPriceTargetConsensusDto();
        }
        CompletableFuture<DiscountedCashFlowDTO> dcfDtoFuture = null;
        CompletableFuture<PriceTargetSummaryDTO> ptsDtoFuture = null;
        CompletableFuture<PriceTargetConsensusDTO> ptcDtoFuture = null;
        //start the missing ones asynchronously
        if (dcfDto == null){
            dcfDtoFuture = CompletableFuture.supplyAsync(()->this.fmpResponseHandler.getDiscountedCashFlowReportFromFmpApi(ticker));
        }
        if (ptsDto == null) {
            ptsDtoFuture = CompletableFuture.supplyAsync(()->this.fmpResponseHandler.getPriceTargetSummaryReportFromFmpApi(ticker));
        }
        if (ptcDto == null){
            ptcDtoFuture = CompletableFuture.supplyAsync(()->this.fmpResponseHandler.getPriceTargetConsensusReportFromFmpApi(ticker));
        }
        try {
            //but we have to block before returning to scrape all the missing data we can
            dcfDto = dcfDto == null ? dcfDtoFuture.get(timeOutInSeconds, TimeUnit.MILLISECONDS) : dcfDto;
            ptsDto = ptsDto == null ? ptsDtoFuture.get(timeOutInSeconds, TimeUnit.MILLISECONDS) : ptsDto;
            ptcDto = ptcDto == null ? ptcDtoFuture.get(timeOutInSeconds, TimeUnit.MILLISECONDS) : ptcDto;
        } catch (final InterruptedException interruptedException) {
            LOG.error("Unexpected interruption while getting data from the FMP api for ticker {}!", ticker, interruptedException);
            Thread.currentThread().interrupt();
        } catch (final ExecutionException executionException) {
            //we must throw the cause of the execution exception because it may be api key issue
            throw executionException.getCause();
        } catch (final TimeoutException timeoutException) {
            LOG.error("Circuit breaker timeout while trying to get data from the FMP api for ticker {}!", ticker, timeoutException);
        }
        //as this is the last step, we return what we have, even if it's all null
        return RecordHolder.newRecordHolder(ticker, dcfDto, ptcDto, ptsDto);
    }

    @NotNull
    private ValuationResponse handleDbError(final String ticker, final Exception illegalStateException){
        LOG.error("Querying the database encountered a fatal data consistency issue for ticker {}! Returning http 500 as a response!", ticker, illegalStateException);
        return new ValuationResponse.Builder()
                .statusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode())
                .errorMessage("The server encountered an unexpected internal error when trying to generate report for ticker {}!")
                .build();
    }

    @NotNull
    private ValuationResponse handleFmpApiError(final RecordHolder recordHolder, final Throwable throwable){
        final boolean noRecord = recordHolder == null;
        if (throwable instanceof final ApiKeyException ex){
            return this.translateExceptedExceptions(noRecord, recordHolder, HttpStatusCode.UNAUTHORIZED.getStatusCode(), ex);
        } else if (throwable instanceof final RateLimitReachedException rre) {
            return this.translateExceptedExceptions(noRecord, recordHolder, HttpStatusCode.TOO_MANY_REQUESTS.getStatusCode(), rre);
        } else if (noRecord) {
            LOG.error("Encountered unexpected exception while getting data from FMP Api!", throwable);
            return new ValuationResponse.Builder()
                    .statusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode())
                    .errorMessage("Internal server error, please try again later!")
                    .build();
        } else {
            //LOG for internal use but return the partial data we have
            LOG.error("Encountered unexpected exception while getting data from FMP Api!", throwable);
            return new ValuationResponse.Builder()
                    .recordHolder(recordHolder)
                    .responseBodyFormatter(this.formatter)
                    .statusCode(HttpStatusCode.OK.getStatusCode())
                    .build();
        }
    }

    @NotNull
    private ValuationResponse translateExceptedExceptions(final boolean noRecord, final RecordHolder recordHolder, final int httpStatusCode, final RuntimeException rte){
        if (noRecord) {
            return new ValuationResponse.Builder()
                    .errorMessage(rte.getMessage())
                    .statusCode(httpStatusCode)
                    .build();
        } else {
            //in this case we return the data we have but the error message from the API too
            return new ValuationResponse.Builder()
                    .recordHolder(recordHolder)
                    .responseBodyFormatter(this.formatter)
                    .statusCode(HttpStatusCode.OK.getStatusCode())
                    .errorMessage(rte.getMessage())
                    .build();
        }
    }
}
