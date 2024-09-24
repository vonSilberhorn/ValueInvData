package com.szilberhornz.valueinvdata.services.stockvaluation.fmp;

import com.szilberhornz.valueinvdata.services.stockvaluation.core.HttpClientFactory;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr.FMPAuthorizer;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr.NoApiKeyFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FMPHttpApiClientTest {

    private FMPApiHttpClient sut;

    private final HttpClientFactory httpClientFactoryMock = Mockito.mock(HttpClientFactory.class);
    private final FMPAuthorizer authorizerMock = Mockito.mock(FMPAuthorizer.class);

    private final ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    @Test
    void testDiscountedCashFlowRequestSuccess() throws NoApiKeyFoundException, IOException, InterruptedException {
        final char[] apiKeyMock = new char[]{'a','b','c','d','e','f'};
        when(this.authorizerMock.retrieveApiKey()).thenReturn(apiKeyMock);
        final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
        when(this.httpClientFactoryMock.newDefaultHttpClient()).thenReturn(httpClientMock);
        this.sut = new FMPApiHttpClient(this.authorizerMock, this.httpClientFactoryMock);
        this.sut.getDiscountedCashFlow("AAPL");
        verify(httpClientMock).send(this.requestCaptor.capture(), any());
        final HttpRequest sentRequest = this.requestCaptor.getValue();
        assertEquals("https://financialmodelingprep.com/api/v3/discounted-cash-flow/AAPL?apikey=abcdef", sentRequest.uri().toString());
    }

    @Test
    void testGetPriceTargetConsensusRequestSuccess() throws NoApiKeyFoundException, IOException, InterruptedException {
        final char[] apiKeyMock = new char[]{'a','b','c','d','e','f'};
        when(this.authorizerMock.retrieveApiKey()).thenReturn(apiKeyMock);
        final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
        when(this.httpClientFactoryMock.newDefaultHttpClient()).thenReturn(httpClientMock);
        this.sut = new FMPApiHttpClient(this.authorizerMock, this.httpClientFactoryMock);
        this.sut.getPriceTargetConsensus("AAPL");
        verify(httpClientMock).send(this.requestCaptor.capture(), any());
        final HttpRequest sentRequest = this.requestCaptor.getValue();
        assertEquals("https://financialmodelingprep.com/api/v4/price-target-consensus?symbol=AAPL&apikey=abcdef", sentRequest.uri().toString());
    }

    @Test
    void testGetPriceTargetSummaryRequestSuccess() throws NoApiKeyFoundException, IOException, InterruptedException {
        final char[] apiKeyMock = new char[]{'a','b','c','d','e','f'};
        when(this.authorizerMock.retrieveApiKey()).thenReturn(apiKeyMock);
        final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
        when(this.httpClientFactoryMock.newDefaultHttpClient()).thenReturn(httpClientMock);
        this.sut = new FMPApiHttpClient(this.authorizerMock, this.httpClientFactoryMock);
        this.sut.getPriceTargetSummary("AAPL");
        verify(httpClientMock).send(this.requestCaptor.capture(), any());
        final HttpRequest sentRequest = this.requestCaptor.getValue();
        assertEquals("https://financialmodelingprep.com/api/v4/price-target-summary?symbol=AAPL&apikey=abcdef", sentRequest.uri().toString());
    }

    @Test
    void testDiscountedCashFlowRequestIOException() throws NoApiKeyFoundException, IOException, InterruptedException {
        final char[] apiKeyMock = new char[]{'a','b','c','d','e','f'};
        when(this.authorizerMock.retrieveApiKey()).thenReturn(apiKeyMock);
        final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
        when(httpClientMock.send(any(), any())).thenThrow(new IOException("Oh no! Anyway..."));
        when(this.httpClientFactoryMock.newDefaultHttpClient()).thenReturn(httpClientMock);
        this.sut = new FMPApiHttpClient(this.authorizerMock, this.httpClientFactoryMock);
        final HttpResponse<String> result = this.sut.getDiscountedCashFlow("AAPL");
        assertNull(result);
    }

    @Test
    void testDiscountedCashFlowRequestInterruptedException() throws NoApiKeyFoundException, IOException, InterruptedException {
        final char[] apiKeyMock = new char[]{'a','b','c','d','e','f'};
        when(this.authorizerMock.retrieveApiKey()).thenReturn(apiKeyMock);
        final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
        when(httpClientMock.send(any(), any())).thenThrow(new InterruptedException("Oh no! Are we shutting down?..."));
        when(this.httpClientFactoryMock.newDefaultHttpClient()).thenReturn(httpClientMock);
        this.sut = new FMPApiHttpClient(this.authorizerMock, this.httpClientFactoryMock);
        final HttpResponse<String> result = this.sut.getDiscountedCashFlow("AAPL");
        assertNull(result);
    }

    @Test
    void testDiscountedCashFlowRequestNoApiKeyFoundException() throws NoApiKeyFoundException, IOException, InterruptedException {
        final String noApiKeyExceptionMessage = "Test exception!";
        final NoApiKeyFoundException exception = new NoApiKeyFoundException(noApiKeyExceptionMessage);
        when(this.authorizerMock.retrieveApiKey()).thenThrow(exception);
        final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
        when(this.httpClientFactoryMock.newDefaultHttpClient()).thenReturn(httpClientMock);
        this.sut = new FMPApiHttpClient(this.authorizerMock, this.httpClientFactoryMock);
        final Exception result = assertThrows(NoApiKeyFoundException.class, () -> this.sut.getDiscountedCashFlow("AAPL"));
        assertEquals(noApiKeyExceptionMessage, result.getMessage());
    }
}
