package com.szilberhornz.valueinvdata.services.stockvaluation.core;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.FMPClient;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authorization.NoApiKeyFoundException;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authorization.FMPAuthorization;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

public class SVSHttpHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SVSHttpHandler.class);

    final FMPClient fmpClient;

    //todo this class or a delegate needs to know about the fmpClient, the cache and the DataSource
    //this class should probably delegate work: if the app will only have one aggregator endpoint, then this class
    //will need to asynchronously delegate tasks and implement circuit breaking. If I decide to have multiple
    //endpoints instead, then it will have to delegate work based on the endpoints.

    //todo combine with price target summary https://www.investopedia.com/terms/p/pricetarget.asp
    // also add the price target consensus
    public SVSHttpHandler(FMPClient fmpClient) {
        this.fmpClient = fmpClient;
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        LOGGER.info("Received http request {} {}",  exchange.getRequestMethod(), exchange.getRequestURI());
        String response;
        try {
            String apiKey = this.getFMPApiKeyFromRequestUri(exchange.getRequestURI());
            response = this.fmpClient.getDiscountedCashFlow("AAPL", apiKey).toString();

            exchange.sendResponseHeaders(200, response.getBytes().length);
            LOGGER.info("Sending back response body: {}", response);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (RuntimeException | NoApiKeyFoundException runtimeException) {
            response = runtimeException.getMessage();
            exchange.sendResponseHeaders(401,response.getBytes().length);
            LOGGER.error(runtimeException.getMessage());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

    }

    /**
        This method exists only for convenience so people can send the api key via the url for testing.
        In a prod environment, the api key would be stored in a secret store. \
        The {@link FMPAuthorization}
        interface intended to kind of emulate that, but of course we don't have a Vault here.
    */
    @Nullable
    private String getFMPApiKeyFromRequestUri (final URI uri) {
        String uriString = uri.toString();
        if (uriString.contains("apikey=")){
            return uriString.substring(uriString.lastIndexOf("apikey=") + 1);
        } else {
            return null;
        }
    }
}
