package com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr;

/**
 * Wrapper for exceptions coming from the FMP api that are related to api key issues
 */
public class ApiKeyException extends RuntimeException {

    public ApiKeyException(final String message) {
        super(message);
    }
}
