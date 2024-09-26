package com.szilberhornz.valueinvdata.services.stockvaluation.core;

import com.sun.net.httpserver.HttpExchange;
import com.szilberhornz.valueinvdata.services.stockvaluation.cache.RecordHolder;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.ValuationReport;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.ValuationReportRestController;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

class StockValuationServiceHttpHandlerTest {

    private final ValuationReportRestController restControllerMock = Mockito.mock(ValuationReportRestController.class);

    @Test
    void testInvalidRequestMethod() throws IOException {
        final HttpExchange exchangeMock = Mockito.mock(HttpExchange.class);
        Mockito.when(exchangeMock.getRequestMethod()).thenReturn("POST");
        final OutputStream osMock = Mockito.mock(OutputStream.class);
        Mockito.when(exchangeMock.getResponseBody()).thenReturn(osMock);
        final StockValuationServiceHttpHandler sut = new StockValuationServiceHttpHandler(this.restControllerMock);
        sut.handle(exchangeMock);
        Mockito.verify(exchangeMock, Mockito.times(1)).sendResponseHeaders(404, 88L);
    }

    @Test
    void testInvalidRequestURI() throws IOException {
        final URI requestURI = URI.create("/random-invalid?ticker=TICKER");
        final HttpExchange exchangeMock = Mockito.mock(HttpExchange.class);
        Mockito.when(exchangeMock.getRequestMethod()).thenReturn("GET");
        Mockito.when(exchangeMock.getRequestURI()).thenReturn(requestURI);
        final OutputStream osMock = Mockito.mock(OutputStream.class);
        Mockito.when(exchangeMock.getResponseBody()).thenReturn(osMock);
        final StockValuationServiceHttpHandler sut = new StockValuationServiceHttpHandler(this.restControllerMock);
        sut.handle(exchangeMock);
        Mockito.verify(exchangeMock, Mockito.times(1)).sendResponseHeaders(404, 88L);
    }

    @Test
    void testInvalidRequestURI2() throws IOException {
        final URI requestURI = URI.create("/valuation-report?symbol=TICKER");
        final HttpExchange exchangeMock = Mockito.mock(HttpExchange.class);
        Mockito.when(exchangeMock.getRequestMethod()).thenReturn("GET");
        Mockito.when(exchangeMock.getRequestURI()).thenReturn(requestURI);
        final OutputStream osMock = Mockito.mock(OutputStream.class);
        Mockito.when(exchangeMock.getResponseBody()).thenReturn(osMock);
        final StockValuationServiceHttpHandler sut = new StockValuationServiceHttpHandler(this.restControllerMock);
        sut.handle(exchangeMock);
        Mockito.verify(exchangeMock, Mockito.times(1)).sendResponseHeaders(404, 88L);
    }

    @Test
    void testWithValidURI() throws IOException {
        final URI requestURI = URI.create("/valuation-report?ticker=TICKER");
        final HttpExchange exchangeMock = Mockito.mock(HttpExchange.class);
        Mockito.when(exchangeMock.getRequestMethod()).thenReturn("GET");
        Mockito.when(exchangeMock.getRequestURI()).thenReturn(requestURI);
        final OutputStream osMock = Mockito.mock(OutputStream.class);
        Mockito.when(exchangeMock.getResponseBody()).thenReturn(osMock);
        final DiscountedCashFlowDTO dcfDto = new DiscountedCashFlowDTO("DUMMY", "2024-09-26", 15.5, 14);
        final PriceTargetConsensusDTO ptcDto = new PriceTargetConsensusDTO("DUMMY", 20, 10, 16, 15);
        final PriceTargetSummaryDTO ptsDto = new PriceTargetSummaryDTO("DUMMY", 2, 16, 5, 14);
        final ValuationReport report = new ValuationReport.Builder()
                .statusCode(HttpStatusCode.OK.getStatusCode())
                .recordHolder(RecordHolder.newRecordHolder("TICKER", dcfDto, ptcDto, ptsDto))
                .build();
        Mockito.when(this.restControllerMock.getValuationReport("TICKER")).thenReturn(report);
        final StockValuationServiceHttpHandler sut = new StockValuationServiceHttpHandler(this.restControllerMock);
        sut.handle(exchangeMock);
        Mockito.verify(exchangeMock, Mockito.times(1)).sendResponseHeaders(200, 204L);
    }
}
