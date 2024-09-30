package com.szilberhornz.valueinvdata.services.stockvaluation.fmp;

import com.szilberhornz.valueinvdata.services.stockvaluation.model.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.model.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.model.record.PriceTargetSummaryDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.fmp.FMPApiHttpClient;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.fmp.FMPResponseHandler;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.fmp.RateLimitReachedException;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.fmp.authr.ApiKeyException;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.fmp.authr.InsufficientPrivilegesException;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.fmp.authr.InvalidApiKeyException;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.fmp.authr.NoApiKeyFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class FMPResponseHandlerTest {


    private final FMPApiHttpClient clientMock = Mockito.mock(FMPApiHttpClient.class);
    private FMPResponseHandler sut;

    @Test
    void getDcfReportSuccessfully() throws ApiKeyException, RateLimitReachedException {
        final String validStringResponse = "[\n" +
                "\t{\n" +
                "\t\t\"symbol\": \"AAPL\",\n" +
                "\t\t\"date\": \"2023-03-03\",\n" +
                "\t\t\"dcf\": 151.0983806294802,\n" +
                "\t\t\"Stock Price\": 149.65\n" +
                "\t}\n" +
                "]";
        final HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.body()).thenReturn(validStringResponse);
        when(response.statusCode()).thenReturn(200);
        when(this.clientMock.getDiscountedCashFlow("AAPL")).thenReturn(response);
        this.sut = new FMPResponseHandler(this.clientMock);
        final DiscountedCashFlowDTO result = this.sut.getDiscountedCashFlowReportFromFmpApi("AAPL");
        assertEquals("2023-03-03", result.dateString());
        assertEquals(151.0983806294802, result.dcf());
        assertEquals(149.65, result.stockPrice());
    }

    @Test
    void getDcfReportSuccessfullyWithRetryAfterException() throws ApiKeyException, RateLimitReachedException {
        final String validStringResponse = "[\n" +
                "\t{\n" +
                "\t\t\"symbol\": \"AAPL\",\n" +
                "\t\t\"date\": \"2023-03-03\",\n" +
                "\t\t\"dcf\": 151.0983806294802,\n" +
                "\t\t\"Stock Price\": 149.65\n" +
                "\t}\n" +
                "]";
        final HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.body()).thenReturn(validStringResponse);
        when(response.statusCode()).thenReturn(200);
        //throw exception for first try here!!!
        when(this.clientMock.getDiscountedCashFlow("AAPL")).thenThrow(new RuntimeException("Oops!")).thenReturn(response);
        this.sut = new FMPResponseHandler(this.clientMock);
        final DiscountedCashFlowDTO result = this.sut.getDiscountedCashFlowReportFromFmpApi("AAPL");
        assertEquals("2023-03-03", result.dateString());
        assertEquals(151.0983806294802, result.dcf());
        assertEquals(149.65, result.stockPrice());
    }

    @Test
    void getDcfReportSuccessfullyWithRetryAfterRetryableStatusCode() throws ApiKeyException, RateLimitReachedException {
        final String validStringResponse = "[\n" +
                "\t{\n" +
                "\t\t\"symbol\": \"AAPL\",\n" +
                "\t\t\"date\": \"2023-03-03\",\n" +
                "\t\t\"dcf\": 151.0983806294802,\n" +
                "\t\t\"Stock Price\": 149.65\n" +
                "\t}\n" +
                "]";
        final HttpResponse<String> okResponse = Mockito.mock(HttpResponse.class);
        when(okResponse.body()).thenReturn(validStringResponse);
        when(okResponse.statusCode()).thenReturn(200);
        final HttpResponse<String> retryableResponse = Mockito.mock(HttpResponse.class);
        when(retryableResponse.statusCode()).thenReturn(408);
        //return retryable response first
        when(this.clientMock.getDiscountedCashFlow("AAPL")).thenReturn(retryableResponse).thenReturn(okResponse);
        this.sut = new FMPResponseHandler(this.clientMock);
        final DiscountedCashFlowDTO result = this.sut.getDiscountedCashFlowReportFromFmpApi("AAPL");
        assertEquals("2023-03-03", result.dateString());
        assertEquals(151.0983806294802, result.dcf());
        assertEquals(149.65, result.stockPrice());
    }

    @Test
    void noApiKeyFoundExceptionShouldPropagate() throws ApiKeyException {
        final NoApiKeyFoundException expectedException = new NoApiKeyFoundException("Test exception");
        when(this.clientMock.getDiscountedCashFlow("AAPL")).thenThrow(expectedException);
        this.sut = new FMPResponseHandler(this.clientMock);
        final Exception exception = assertThrows(NoApiKeyFoundException.class, ()-> this.sut.getDiscountedCashFlowReportFromFmpApi("AAPL"));
        assertEquals("Test exception", exception.getMessage());
    }

    @Test
    void getDcfReportShouldFailAfterFailedRetry() throws ApiKeyException, RateLimitReachedException {
        final HttpResponse<String> retryableResponse = Mockito.mock(HttpResponse.class);
        when(retryableResponse.statusCode()).thenReturn(408).thenReturn(408);
        //return two retryable responses
        when(this.clientMock.getDiscountedCashFlow("AAPL")).thenReturn(retryableResponse).thenReturn(retryableResponse);
        this.sut = new FMPResponseHandler(this.clientMock);
        final DiscountedCashFlowDTO result = this.sut.getDiscountedCashFlowReportFromFmpApi("AAPL");
        assertNull(result);
    }

    @Test
    void getPtsReportShouldFailAfterFailedRetry() throws ApiKeyException, RateLimitReachedException {
        final HttpResponse<String> retryableResponse = Mockito.mock(HttpResponse.class);
        when(retryableResponse.statusCode()).thenReturn(408).thenReturn(408);
        //return two retryable responses
        when(this.clientMock.getPriceTargetSummary("AAPL")).thenReturn(retryableResponse).thenReturn(retryableResponse);
        this.sut = new FMPResponseHandler(this.clientMock);
        final PriceTargetSummaryDTO result = this.sut.getPriceTargetSummaryReportFromFmpApi("AAPL");
        assertNull(result);
    }

    @Test
    void getDcfReportShouldThrowRateLimitReachedExceptionForHttp429() throws ApiKeyException, RateLimitReachedException {
        final HttpResponse<String> tooManyRequestsResponse = Mockito.mock(HttpResponse.class);
        when(tooManyRequestsResponse.statusCode()).thenReturn(429);
        when(this.clientMock.getDiscountedCashFlow("AAPL")).thenReturn(tooManyRequestsResponse);
        this.sut = new FMPResponseHandler(this.clientMock);
        final Exception exception = assertThrows(RateLimitReachedException.class, ()-> this.sut.getDiscountedCashFlowReportFromFmpApi("AAPL"));
        assertEquals("Daily rate limit reached for the supplied api key!", exception.getMessage());
    }

    @Test
    void getPtcReportShouldFailAfterFailedRetry() throws ApiKeyException, RateLimitReachedException {
        //return two null responses
        when(this.clientMock.getDiscountedCashFlow("AAPL")).thenReturn(null).thenReturn(null);
        this.sut = new FMPResponseHandler(this.clientMock);
        final PriceTargetConsensusDTO result = this.sut.getPriceTargetConsensusReportFromFmpApi("AAPL");
        assertNull(result);
    }

    @Test
    void getPtcReportSuccessfully() throws ApiKeyException, RateLimitReachedException {
        final String validStringResponse = "[\n" +
                "\t{\n" +
                "\t\t\"symbol\": \"AAPL\",\n" +
                "\t\t\"targetHigh\": 240,\n" +
                "\t\t\"targetLow\": 110,\n" +
                "\t\t\"targetConsensus\": 189.18,\n" +
                "\t\t\"targetMedian\": 195\n" +
                "\t}\n" +
                "]";
        final HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.body()).thenReturn(validStringResponse);
        when(response.statusCode()).thenReturn(200);
        when(this.clientMock.getPriceTargetConsensus("AAPL")).thenReturn(response);
        this.sut = new FMPResponseHandler(this.clientMock);
        final PriceTargetConsensusDTO result = this.sut.getPriceTargetConsensusReportFromFmpApi("AAPL");
        assertEquals(240, result.targetHigh());
        assertEquals(110, result.targetLow());
        assertEquals(189.18, result.targetConsensus());
        assertEquals(195, result.targetMedian());
    }

    @Test
    void getPtsReportSuccessfully() throws ApiKeyException, RateLimitReachedException {
        final String validStringResponse = "[\n" +
                "\t{\n" +
                "\t\t\"symbol\": \"AAPL\",\n" +
                "\t\t\"lastMonth\": 5,\n" +
                "\t\t\"lastMonthAvgPriceTarget\": 220.2,\n" +
                "\t\t\"lastQuarter\": 11,\n" +
                "\t\t\"lastQuarterAvgPriceTarget\": 217.18,\n" +
                "\t\t\"lastYear\": 46,\n" +
                "\t\t\"lastYearAvgPriceTarget\": 186.8,\n" +
                "\t\t\"allTime\": 113,\n" +
                "\t\t\"allTimeAvgPriceTarget\": 186.31,\n" +
                "\t\t\"publishers\": \"[\\\"Benzinga\\\",\\\"TheFly\\\",\\\"Pulse 2.0\\\",\\\"MarketWatch\\\",\\\"TipRanks Contributor\\\",\\\"Investing\\\",\\\"StreetInsider\\\",\\\"Barrons\\\",\\\"Investor's Business Daily\\\"]\"\n" +
                "\t}\n" +
                "]";
        final HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.body()).thenReturn(validStringResponse);
        when(response.statusCode()).thenReturn(200);
        when(this.clientMock.getPriceTargetSummary("AAPL")).thenReturn(response);
        this.sut = new FMPResponseHandler(this.clientMock);
        final PriceTargetSummaryDTO result = this.sut.getPriceTargetSummaryReportFromFmpApi("AAPL");
        assertEquals(5, result.lastMonth());
        assertEquals(220.2, result.lastMonthAvgPriceTarget());
        assertEquals(11, result.lastQuarter());
        assertEquals(217.18, result.lastQuarterAvgPriceTarget());
    }

    @Test
    void invalidApiKeyExceptionShouldBeThrown() throws ApiKeyException {
        final String responseBodyString = "Invalid API KEY. Feel free to create a Free API Key or visit " +
                "https://site.financialmodelingprep.com/faqs?search=why-is-my-api-key-invalid for more information.";
        final HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.body()).thenReturn(responseBodyString);
        when(response.statusCode()).thenReturn(401);
        when(this.clientMock.getPriceTargetConsensus("AAPL")).thenReturn(response);
        this.sut = new FMPResponseHandler(this.clientMock);
        final Exception exception = assertThrows(InvalidApiKeyException.class, ()-> this.sut.getPriceTargetConsensusReportFromFmpApi("AAPL"));
        assertEquals(responseBodyString, exception.getMessage());
    }

    @Test
    void insufficientPrivilegesExceptionShouldBeThrown() throws ApiKeyException {
        final String responseBodyString = "Special Endpoint : This endpoint is not available under your current subscription " +
                "please visit our subscription page to upgrade your plan at https://site.financialmodelingprep.com/developer/docs/pricing";
        final HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.body()).thenReturn(responseBodyString);
        when(response.statusCode()).thenReturn(403);
        when(this.clientMock.getPriceTargetConsensus("AAPL")).thenReturn(response);
        this.sut = new FMPResponseHandler(this.clientMock);
        final Exception exception = assertThrows(InsufficientPrivilegesException.class, ()-> this.sut.getPriceTargetConsensusReportFromFmpApi("AAPL"));
        assertEquals(responseBodyString, exception.getMessage());
    }
}
