package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.RecordHolder;
import com.szilberhornz.valueinvdata.services.stockvaluation.cache.TickerCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.HttpStatusCode;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.RateLimitReachedException;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr.ApiKeyException;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.circuitbreaker.VRSagaCircuitBreaker;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.formatter.ValuationResponseBodyFormatter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The orchestrator class for the ValuationReport saga. Responsible for receiving the http request coming on
 * /valuation-report?ticker=TICKER and generating report for the requested TICKER.
 * It first tries the cache, then the database and tries to plug in any missing data from the FMP api, then
 * send all the new data back to persistence and cache. Uses simple circuit breaker logic for timeouts, and also
 * relies heavily on asynchronous, parallel execution using CompletableFuture instances.
 */
public class VRSagaOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(VRSagaOrchestrator.class);

    private static final String INVALID_TICKER_MESSAGE = "The server only responds to valuation report requests " +
            "for real tickers! The ticker %s is not a valid ticker! Please try again with a valid ticker!";
    private final TickerCache tickerCache;
    private final ValuationResponseBodyFormatter formatter;
    private final VRSagaCircuitBreaker circuitBreaker;
    private final VRSagaDataBroker dataBroker;

    public VRSagaOrchestrator(final TickerCache tickerCache, final ValuationResponseBodyFormatter formatter,
                              final VRSagaCircuitBreaker circuitBreaker, final VRSagaDataBroker dataBroker) {
        this.tickerCache = tickerCache;
        this.formatter = formatter; // must use this with http 200
        this.circuitBreaker = circuitBreaker;
        this.dataBroker = dataBroker;
    }

    public ValuationReport getValuationResponse(final String ticker) {
        try {
            final CompletableFuture<ValuationReport> responseFuture = CompletableFuture.supplyAsync(() -> this.generateValueReport(ticker.toUpperCase(Locale.ROOT)));
            return responseFuture.get(this.circuitBreaker.getOverallTimeoutInMillis(), TimeUnit.MILLISECONDS);
        } catch (final InterruptedException interruptedException) {
            LOG.error("Unexpected interruption while generating report for ticker {}", ticker, interruptedException);
            Thread.currentThread().interrupt();
            return this.returnInternalError(ticker);
        } catch (final TimeoutException timeoutException) {
            LOG.error("Circuit breaker timeout while generating report for ticker {}", ticker, timeoutException);
            return this.returnInternalError(ticker);
        } catch (final Throwable throwable) {
            LOG.error("Unexpected exception while generating report for ticker {}", ticker, throwable);
            return this.returnInternalError(ticker);
        }
    }

    @NotNull
    private ValuationReport generateValueReport(final String upperCaseTicker) {
        if (!this.tickerCache.tickerExists(upperCaseTicker)) { //the easy way out
            return this.respondToInvalidTicker(upperCaseTicker);
        }
        //try to get report RecordHolder from cache, then from db then from FMP API
        final RecordHolder recordFromCache = this.dataBroker.getFromCache(upperCaseTicker);
        final RecordHolder recordFromDb;
        if (recordFromCache != null && !recordFromCache.isDataMissing()) { //a quick win
            LOG.info("Valuation report for ticker {} generated from in-memory cache", upperCaseTicker);
            return new ValuationReport.Builder()
                    .recordHolder(recordFromCache)
                    .responseBodyFormatter(this.formatter)
                    .statusCode(HttpStatusCode.OK.getStatusCode())
                    .build();
        } else {
            //this record may contain fatal error, we must check for that
            recordFromDb = this.getRecordFromDatabase(upperCaseTicker, recordFromCache);
            if (recordFromDb != null && recordFromDb.getCauseOfNullDtos() != null && recordFromDb.getCauseOfNullDtos() instanceof final IllegalStateException ise) {
                return this.handleDbError(upperCaseTicker, ise);
            }
        }
        if (recordFromDb != null && !recordFromDb.isDataMissing()){
            LOG.info("Valuation report for ticker {} generated from database", upperCaseTicker);
            return new ValuationReport.Builder()
                    .recordHolder(recordFromDb)
                    .responseBodyFormatter(this.formatter)
                    .statusCode(HttpStatusCode.OK.getStatusCode())
                    .build();
        } else {
            return this.completeReportFromFmpApi(upperCaseTicker, recordFromDb, recordFromCache);
        }
    }

    @NotNull
    private ValuationReport completeReportFromFmpApi(final String upperCaseTicker, final RecordHolder recordFromDb, final RecordHolder recordFromCache) {
        final RecordHolder recordFromFmpApi = this.dataBroker.getDataFromFmpApi(recordFromDb, upperCaseTicker, this.circuitBreaker.getTimeoutForApiCallInMillis());
        if (recordFromFmpApi.getCauseOfNullDtos() == null){
            LOG.info("Valuation report for ticker {} generated from the FMP api", upperCaseTicker);
            //before returning, make sure to start another thread to persist the data from the FMP api to db and cache!
            this.cacheAndPersistAnyNewData(upperCaseTicker, recordFromCache, recordFromDb, recordFromFmpApi);
            return new ValuationReport.Builder()
                    .statusCode(HttpStatusCode.OK.getStatusCode())
                    .recordHolder(recordFromFmpApi)
                    .responseBodyFormatter(this.formatter)
                    .build();
        } else {
            //else we need to deal with the exceptions coming from the FMP api
            if (recordFromDb == null && recordFromFmpApi.getDtoCount() == 0) {
                LOG.warn("No report was generated for ticker {}! No data found in database and only received error messages from FMP api, sending back the appropriate response to the caller!", upperCaseTicker);
            } else if (recordFromDb != null && (recordFromDb.getDtoCount() == recordFromFmpApi.getDtoCount())) {
                LOG.warn("Valuation report for ticker {} generated from actual data from database and error messages from FMP api", upperCaseTicker);
            } else {
                LOG.warn("Valuation report for ticker {} generated from FMP Api partial data and error messages", upperCaseTicker);
            }
            //same as above, before returning, make sure to start another thread to persist the data from the FMP api to db and cache!
            this.cacheAndPersistAnyNewData(upperCaseTicker, recordFromCache, recordFromDb, recordFromFmpApi);
            //we handle error and return what we can (that is what we have from the db which is equals or a superset of what we have from the cache)
            return this.handleFmpApiError(recordFromFmpApi, recordFromFmpApi.getCauseOfNullDtos(), upperCaseTicker);
        }
    }

    private void cacheAndPersistAnyNewData(final String upperCaseTicker, final RecordHolder recordFromCache, final RecordHolder recordFromDb, final RecordHolder recordFromFmpApi) {
        CompletableFuture.runAsync(() -> this.dataBroker.persistData(upperCaseTicker, recordFromCache, recordFromDb, recordFromFmpApi));
    }

    @Nullable
    private RecordHolder getRecordFromDatabase(final String upperCaseTicker, final RecordHolder recordFromCache) {
        RecordHolder recordFromDb = null;
        try {
            //this fills up missing data if it can
            final CompletableFuture<RecordHolder> rhFuture = CompletableFuture.supplyAsync(() -> this.dataBroker.getDataFromDb(recordFromCache, upperCaseTicker));
            //wait till timeout or success, we go to the FMP Api only if we are still missing data.
            recordFromDb = rhFuture.completeOnTimeout(recordFromCache, this.circuitBreaker.getTimeoutForDbQueryInMillis(), TimeUnit.MILLISECONDS).get();
        } catch (final ExecutionException executionException) {
            //this is a fatal error, we must handle it and return http 500. This means that our database tables contain more columns of data
            //than our record classes have - for this very reason it should never even happen to begin with.
            if (executionException.getCause() instanceof final IllegalStateException ise) {
                if (recordFromCache != null){
                    recordFromDb = RecordHolder.newRecordHolder(upperCaseTicker, recordFromCache.getDiscountedCashFlowDto(), recordFromCache.getPriceTargetConsensusDto(), recordFromCache.getPriceTargetSummaryDto(), ise);
                } else {
                    recordFromDb = RecordHolder.newRecordHolder(upperCaseTicker, null, null, null, ise);
                }
            }
            //else log and move on, the expected IllegalStateException is already handled
            LOG.error("Unexpected exception happened while trying to get data for ticker {} from the database!", upperCaseTicker, executionException.getCause());
        } catch (final InterruptedException interruptedException) {
            LOG.error("Unexpected thread interruption while trying to get data for ticker {} from the database!", upperCaseTicker, interruptedException);
            Thread.currentThread().interrupt();
        }
        return recordFromDb;
    }

    private ValuationReport respondToInvalidTicker(final String ticker) {
        LOG.warn("Received illegal request for invalid ticker {}! Sending back http 403", ticker);
        final String errorMessage = String.format(INVALID_TICKER_MESSAGE, ticker);
        return new ValuationReport.Builder()
                .statusCode(HttpStatusCode.FORBIDDEN.getStatusCode())
                .errorMessage(errorMessage)
                .build();
    }


    @NotNull
    private ValuationReport handleDbError(final String ticker, final Exception illegalStateException) {
        LOG.error("Querying the database encountered a fatal data consistency issue for ticker {}! Returning http 500 as a response!", ticker, illegalStateException);
        return this.returnInternalError(ticker);
    }

    private ValuationReport returnInternalError(final String ticker) {
        return new ValuationReport.Builder()
                .statusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode())
                .errorMessage("The server encountered an unexpected internal error when trying to generate report for ticker " + ticker + "!")
                .build();
    }

    @NotNull
    private ValuationReport handleFmpApiError(final RecordHolder recordHolder, final Throwable throwable, final String ticker) {
        final boolean noRecord = recordHolder == null || recordHolder.getDtoCount() == 0;
        if (throwable instanceof final ApiKeyException ex) {
            return this.translateExceptedExceptions(noRecord, recordHolder, HttpStatusCode.UNAUTHORIZED.getStatusCode(), ex);
        } else if (throwable instanceof final RateLimitReachedException rre) {
            return this.translateExceptedExceptions(noRecord, recordHolder, HttpStatusCode.TOO_MANY_REQUESTS.getStatusCode(), rre);
        } else if (noRecord) {
            LOG.error("Encountered unexpected exception while getting data from FMP Api!", throwable);
            return this.returnInternalError(ticker);
        } else {
            //LOG for internal use but return the partial data we have, and doesn't leak unknown errors
            LOG.error("Encountered unexpected exception while getting data from FMP Api!", throwable);
            return new ValuationReport.Builder()
                    .recordHolder(recordHolder)
                    .responseBodyFormatter(this.formatter)
                    .statusCode(HttpStatusCode.OK.getStatusCode())
                    .build();
        }
    }

    @NotNull
    private ValuationReport translateExceptedExceptions(final boolean noRecord, final RecordHolder recordHolder, final int httpStatusCode, final RuntimeException rte) {
        if (noRecord) {
            return new ValuationReport.Builder()
                    .errorMessage(rte.getMessage())
                    .statusCode(httpStatusCode)
                    .build();
        } else {
            //in this case we return the data we have but the error message from the API too
            return new ValuationReport.Builder()
                    .recordHolder(recordHolder)
                    .responseBodyFormatter(this.formatter)
                    .statusCode(HttpStatusCode.OK.getStatusCode())
                    .errorMessage(rte.getMessage())
                    .build();
        }
    }
}
