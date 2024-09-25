package com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr;

public class NoApiKeyFoundException extends ApiKeyException {

    public NoApiKeyFoundException(final String message) {
        super(message);
    }
}
