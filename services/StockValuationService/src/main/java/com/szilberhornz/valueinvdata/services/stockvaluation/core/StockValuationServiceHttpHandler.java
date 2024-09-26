package com.szilberhornz.valueinvdata.services.stockvaluation.core;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.ValuationReport;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.ValuationReportRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class StockValuationServiceHttpHandler implements HttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(StockValuationServiceHttpHandler.class);

    private static final String INVALID_REQUEST = "Invalid request, only /valuation-report?ticker=TICKER format GET requests are supported!";

    final ValuationReportRestController valuationReportRestController;

    public StockValuationServiceHttpHandler(final ValuationReportRestController valuationReportRestController) {
        this.valuationReportRestController = valuationReportRestController;
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        LOG.info("Received http request {} {}", exchange.getRequestMethod(), exchange.getRequestURI());
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")
                || !exchange.getRequestURI().getPath().equalsIgnoreCase("/valuation-report")
                || !exchange.getRequestURI().getQuery().startsWith("ticker=")){
            this.sendResponse(exchange, HttpStatusCode.NOT_FOUND.getStatusCode(), INVALID_REQUEST);
        } else {
            final String ticker = exchange.getRequestURI().getQuery().split("=")[1];
            final ValuationReport valuationReport = this.valuationReportRestController.getValuationReport(ticker);
            final String message = valuationReport.getMessageBody() == null ? valuationReport.getErrorMessage() : valuationReport.getMessageBody();
            this.sendResponse(exchange, valuationReport.getStatusCode(), message);
        }
    }

    private void sendResponse(final HttpExchange exchange, final int statusCode, final String message) throws IOException {
        LOG.info("Sending response with status code {} and message body {}", statusCode, message);
        exchange.sendResponseHeaders(statusCode, message.getBytes(StandardCharsets.UTF_8).length);
        final OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(message.getBytes(StandardCharsets.UTF_8));
        outputStream.close();
    }
}
