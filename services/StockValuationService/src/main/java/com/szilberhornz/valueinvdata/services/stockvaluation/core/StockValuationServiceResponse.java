package com.szilberhornz.valueinvdata.services.stockvaluation.core;

public interface StockValuationServiceResponse {

    int getStatusCode();
    String getMessageBody();
    String getErrorMessage();
}
