package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.fmp.authr;

public class NoApiKeyFoundException extends ApiKeyException {

    public NoApiKeyFoundException(final String message) {
        super(message);
    }
}
