package com.szilberhornz.valueinvdata.services.stockvaluation.fmp;

import com.szilberhornz.valueinvdata.services.stockvaluation.core.HttpClientFactory;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr.FMPAuthorizer;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr.NoApiKeyFoundException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * In this client I use blocking http calls because any method here should be called asynchronously
 * in the first place! In case of unexpected exceptions (IO or Interrupted) I return null so the caller may
 * decide if it wants to retry, possibly along with some response error codes. These exceptions are assumed
 * to be glitches and not total client failures
 */
public class FMPApiHttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(FMPApiHttpClient.class);

    private static final String DCF_ENDPOINT = "https://financialmodelingprep.com/api/v3/discounted-cash-flow/";
    private static final String PTC_ENDPOINT = "https://financialmodelingprep.com/api/v4/price-target-consensus?symbol=";
    private static final String PTS_ENDPOINT = "https://financialmodelingprep.com/api/v4/price-target-summary?symbol=";

    private static final String API_KEY = "?apikey=";
    private static final String API_KEY_AND = "&apikey=";

    private final FMPAuthorizer authorizer;
    private final HttpClientFactory httpClientFactory;

    public FMPApiHttpClient(final FMPAuthorizer authorizer, final HttpClientFactory httpClientFactory) {
        this.authorizer = authorizer;
        this.httpClientFactory = httpClientFactory;
    }

    @Nullable
    public HttpResponse<String> getDiscountedCashFlow(final String ticker) throws NoApiKeyFoundException {
        final String uri = DCF_ENDPOINT +
                ticker +
                API_KEY +
                new String(authorizer.retrieveApiKey());
        final String logMsg = "discounted cashflow";
        return this.getResponse(ticker, uri, logMsg);
    }

    @Nullable
    public HttpResponse<String> getPriceTargetConsensus(final String ticker) throws NoApiKeyFoundException {
        final String uri = PTC_ENDPOINT +
                ticker +
                API_KEY_AND +
                new String(authorizer.retrieveApiKey());
        final String logMsg = "price target consensus";
        return this.getResponse(ticker, uri, logMsg);
    }

    @Nullable
    public HttpResponse<String> getPriceTargetSummary(final String ticker) throws NoApiKeyFoundException {
        final String uri = PTS_ENDPOINT +
                ticker +
                API_KEY_AND +
                new String(authorizer.retrieveApiKey());
        final String logMsg = "price target summary";
        return this.getResponse(ticker, uri, logMsg);
    }

    @Nullable
    private HttpResponse<String> getResponse(final String ticker, final String uri, final String logMsg){
        LOGGER.info("Creating an FMP {} http request for {}", logMsg, ticker);
        final long start = System.nanoTime();
        try (final HttpClient client = this.httpClientFactory.newDefaultHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .GET()
                    .version(HttpClient.Version.HTTP_2)
                    .build();
            LOGGER.info("The {} FMP http request for {} has been sent", logMsg, ticker);
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            final long end = System.nanoTime();
            final long durationInMillis = Duration.ofNanos(end - start).toMillis();
            LOGGER.info("Received {} http response from FMP for {}. The http exchange took {} milliseconds", logMsg, ticker, durationInMillis);
            return response;
        } catch (IOException ioException) {
            LOGGER.error("An unexpected I/O Exception happened while trying to query for {} on the FMP api", ticker, ioException);
        } catch (InterruptedException interruptedException) {
            LOGGER.error("The thread executing the FMP api request got unexpectedly interrupted!");
            Thread.currentThread().interrupt();
        }
        return null;
    }
}
