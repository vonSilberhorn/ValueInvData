package com.szilberhornz.valueinvdata.services.stockvaluation.fmp;

import com.szilberhornz.valueinvdata.services.stockvaluation.AppContext;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.RecordMapper;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr.ApiKeyException;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr.InvalidApiKeyException;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr.NoApiKeyFoundException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.concurrent.Callable;

public class FMPRequestResponseHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FMPRequestResponseHandler.class);

    final FMPApiHttpClient client;

    public FMPRequestResponseHandler(final FMPApiHttpClient client) {
        this.client = client;
    }

    @Nullable
    public DiscountedCashFlowDTO getDiscountedCashFlowReportFromFmpApi(final String ticker) throws RateLimitReachedException, ApiKeyException {
        final String logMsg = "discounted cashflow";
        final HttpResponse<String> response = this.handlePossibleRetry(()-> this.client.getDiscountedCashFlow(ticker), logMsg);
        if (response != null && response.statusCode() == 200){
            return RecordMapper.newDcfDto(response);
        } else {
            this.handleError(response, logMsg);
            return null;
        }
    }

    @Nullable
    public PriceTargetConsensusDTO getPriceTargetConsensusReportFromFmpApi(final String ticker) throws RateLimitReachedException, ApiKeyException {
        final String logMsg = "price target consensus";
        final HttpResponse<String> response = this.handlePossibleRetry(()-> this.client.getPriceTargetConsensus(ticker), logMsg);
        if (response != null && response.statusCode() == 200){
            return RecordMapper.newPtcDto(response);
        } else {
            this.handleError(response, logMsg);
            return null;
        }
    }

    @Nullable
    public PriceTargetSummaryDTO getPriceTargetSummaryReportFromFmpApi(final String ticker) throws RateLimitReachedException, ApiKeyException {
        final String logMsg = "price target summary";
        final HttpResponse<String> response = this.handlePossibleRetry(()-> this.client.getPriceTargetSummary(ticker), logMsg);
        if (response != null && response.statusCode() == 200){
            return RecordMapper.newPtsDto(response);
        } else {
            this.handleError(response, logMsg);
            return null;
        }
    }

    private void handleError(final HttpResponse<String> httpResponse, final String logMsg) throws RateLimitReachedException, ApiKeyException {
        if (httpResponse == null) {
            LOG.error("Couldn't retrieve any response from FMP api for the {} api call, please look for possible reasons in earlier error logs", logMsg);
        } else if (httpResponse.statusCode() == 429) {
            throw new RateLimitReachedException("Daily rate limit reached for the supplied api key!");
        } else if (httpResponse.statusCode() == 401) {
            throw new InvalidApiKeyException(httpResponse.body());
        } else {
            LOG.error("FMP Api returned the following error response {} for the api call: {}", httpResponse, logMsg);
        }
    }

    @Nullable
    private HttpResponse<String> handlePossibleRetry(final Callable<HttpResponse<String>> callable, final String logMsg) throws NoApiKeyFoundException {
        //try api call for the first time
        HttpResponse<String> response = this.tryApiCall(callable, logMsg);
        //try again if needed, but only once
        if (response == null || AppContext.RETRYABLE_HTTP_STATUS_CODES.contains(response.statusCode())){
            if (response == null) {
                LOG.warn("First FMP api call for the {} client method failed, trying again!", logMsg);
            } else {
                LOG.warn("First FMP api call for the {} client method failed with retriable status code {}, trying again!", logMsg, response.statusCode());
            }
            response = this.tryApiCall(callable, logMsg);
        }
        return response;
    }

    //we want to propagate the NoApiKeyFoundException so we can return 401 to the caller
    @Nullable
    private HttpResponse<String> tryApiCall(final Callable<HttpResponse<String>> callable, final String logMsg) throws NoApiKeyFoundException {
        try {
            return callable.call();
        } catch (final NoApiKeyFoundException noApiKeyFoundException) {
            throw noApiKeyFoundException;
        } catch (final Exception e) {
            LOG.error("FMP api call failed for the {} client api method!", logMsg, e);
            return null;
        }
    }
}
