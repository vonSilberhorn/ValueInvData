package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.RecordHolder;
import com.szilberhornz.valueinvdata.services.stockvaluation.cache.TickerCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.HttpStatusCode;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.RateLimitReachedException;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr.ApiKeyException;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.circuitbreaker.VRSagaCircuitBreaker;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.formatter.ValuationResponseBodyFormatter;
import org.jetbrains.annotations.NotNull;
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
 * send as the new data back to persistence and cache. Uses simple circuit breaker logic for timeouts, and also
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

    private ValuationReport generateValueReport(final String upperCaseTicker) {
        if (!this.tickerCache.tickerExists(upperCaseTicker)) {
            LOG.warn("Received illegal request for invalid ticker {}! Sending back http 403", upperCaseTicker);
            return this.respondToInvalidTicker(upperCaseTicker);
        }
        //try to get report RecordHolder from cache, then from db then from FMP API
        final RecordHolder recordFromCache = this.dataBroker.getFromCache(upperCaseTicker);
        RecordHolder recordFromDb = null;
        RecordHolder recordFromFmpApi = null;
        if (recordFromCache == null || recordFromCache.isDataMissing()) {
            try {
                //this fills up missing data if it can
                final CompletableFuture<RecordHolder> rhFuture = CompletableFuture.supplyAsync(() -> this.dataBroker.getDataFromDb(recordFromCache, upperCaseTicker));
                //wait till timeout or success, we go to the FMP Api only if we are still missing data.
                recordFromDb = rhFuture.completeOnTimeout(recordFromCache, this.circuitBreaker.getTimeoutForDbQueryInMillis(), TimeUnit.MILLISECONDS).get();
            } catch (final ExecutionException executionException) {
                if (executionException.getCause() instanceof final IllegalStateException ise) {
                    //we must break here since we should not return data that we know is inconsistent or otherwise wrong
                    return this.handleDbError(upperCaseTicker, ise);
                }
                //else log and move on, the expected IllegalStateException is already handled
                LOG.error("Unexpected exception happened while trying to get data for ticker {} from the database!", upperCaseTicker, executionException.getCause());
            } catch (final InterruptedException interruptedException) {
                LOG.error("Unexpected thread interruption while trying to get data for ticker {} from the database!", upperCaseTicker, interruptedException);
                Thread.currentThread().interrupt();
            }
        } else {
            LOG.info("Valuation report for ticker {} generated from in-memory cache", upperCaseTicker);
            return new ValuationReport.Builder()
                    .recordHolder(recordFromCache)
                    .responseBodyFormatter(this.formatter)
                    .statusCode(HttpStatusCode.OK.getStatusCode())
                    .build();
        }
        final ValuationReport finalResponse;
        if (recordFromDb == null || recordFromDb.isDataMissing()) {
            //this is the most data we are going to have
            recordFromFmpApi = this.dataBroker.getDataFromFmpApi(recordFromDb, upperCaseTicker, this.circuitBreaker.getTimeoutForApiCallInMillis());
            if (recordFromFmpApi.getCauseOfNullDtos() == null){
                LOG.info("Valuation report for ticker {} generated from the FMP api", upperCaseTicker);
                finalResponse = new ValuationReport.Builder()
                        .statusCode(HttpStatusCode.OK.getStatusCode())
                        .recordHolder(recordFromFmpApi)
                        .responseBodyFormatter(this.formatter)
                        .build();
            } else {
                if (recordFromDb == null && recordFromFmpApi.getDtoCount() == 0) {
                    LOG.warn("No report was generated for ticker {}! No data found in database and only received error messages from FMP api, sending back the appropriate response to the caller!", upperCaseTicker);
                } else if (recordFromDb != null && (recordFromDb.getDtoCount() == recordFromFmpApi.getDtoCount())) {
                    LOG.info("Valuation report for ticker {} generated from database and FMP api error messages", upperCaseTicker);
                } else {
                    LOG.info("Valuation report for ticker {} generated from FMP Api partial data and error messages", upperCaseTicker);
                }
                //we handle error and return what we can (that is what we have from the db which is equals or a superset of what we have from the cache)
                finalResponse = this.handleFmpApiError(recordFromFmpApi, recordFromFmpApi.getCauseOfNullDtos(), upperCaseTicker);
            }
        } else {
            LOG.info("Valuation report for ticker {} generated from database", upperCaseTicker);
            finalResponse = new ValuationReport.Builder()
                    .recordHolder(recordFromDb)
                    .responseBodyFormatter(this.formatter)
                    .statusCode(HttpStatusCode.OK.getStatusCode())
                    .build();
        }
        //last step is an async call for persisting all the data if needed
        final RecordHolder finalRecordFromDb = recordFromDb;
        final RecordHolder finalRecordFromFmpApi = recordFromFmpApi;
        CompletableFuture.runAsync(() -> this.dataBroker.persistData(upperCaseTicker, recordFromCache, finalRecordFromDb, finalRecordFromFmpApi));
        //we won't wait for the CompletableFuture to finish
        return finalResponse;
    }

    private ValuationReport respondToInvalidTicker(final String ticker) {
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
