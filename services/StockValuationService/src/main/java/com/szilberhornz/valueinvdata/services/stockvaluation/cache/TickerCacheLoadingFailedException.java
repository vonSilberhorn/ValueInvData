package com.szilberhornz.valueinvdata.services.stockvaluation.cache;

public class TickerCacheLoadingFailedException extends RuntimeException {

    public TickerCacheLoadingFailedException(String message) {
        super(message);
    }

    public TickerCacheLoadingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
