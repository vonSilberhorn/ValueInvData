package com.szilberhornz.valueinvdata.services.stockvaluation.fmp;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authorization.FMPAuthorization;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authorization.NoApiKeyFoundException;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.record.DiscountedCashFlowDTO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FMPClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(FMPClient.class);

    private static final String BASE_URL = "https://financialmodelingprep.com/api/v3";
    private static final String DCF = "/discounted-cash-flow/";

    private final FMPAuthorization authorization;
    private final HttpClient httpClient;
    private final ValuationCache cache;

    public FMPClient(final FMPAuthorization authorization, final HttpClient httpClient, final ValuationCache cache) {
        this.authorization = authorization;
        this.httpClient = httpClient;
        this.cache = cache;
    }


    public DiscountedCashFlowDTO getDiscountedCashFlow(final String ticker, String apiKey) throws NoApiKeyFoundException {
        DiscountedCashFlowDTO dto = cache.getDcfFromCache(ticker);
        if (apiKey == null) {
            apiKey = new String( authorization.retrieveApiKey());
        }
        if (dto == null) {
            final String uri = BASE_URL + DCF + ticker + "?apikey=" + apiKey;
            try (final HttpClient client = HttpClient.newHttpClient()) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(uri))
                        .build();
                LOGGER.info("Request sent");
                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());
                LOGGER.info("Received response");
                JSONArray array = new JSONArray(response.body());
                JSONObject object = array.getJSONObject(0);
                dto = new DiscountedCashFlowDTO(object.getString("symbol"),
                        object.getString("date"), object.getDouble("dcf"), object.getDouble("Stock Price"));
                cache.putIntoDcfCache(ticker, dto);
            } catch (final IOException | InterruptedException ioException) {
                LOGGER.error(ioException.getMessage());
            }
        }
        return dto;
    }
}
