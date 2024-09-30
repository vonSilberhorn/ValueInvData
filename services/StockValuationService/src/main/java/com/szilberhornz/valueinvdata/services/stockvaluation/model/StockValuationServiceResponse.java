package com.szilberhornz.valueinvdata.services.stockvaluation.model;

public interface StockValuationServiceResponse {

    int getStatusCode();
    String getMessageBody();
    String getErrorMessage();
}
