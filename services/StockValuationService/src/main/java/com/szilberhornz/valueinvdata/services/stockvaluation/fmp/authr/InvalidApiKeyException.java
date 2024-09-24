package com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr;

public class InvalidApiKeyException extends ApiKeyException {

    public InvalidApiKeyException(final String message) {
        super(message);
    }
}
