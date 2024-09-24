package com.szilberhornz.valueinvdata.services.stockvaluation.cache;

/**
 * Ticker cache loading failure is illegal state, because there required file is packaged into the jar
 * by default, so where did it go? :)
 */
public class TickerCacheLoadingFailedException extends IllegalStateException {

    public TickerCacheLoadingFailedException(String message) {
        super(message);
    }

    public TickerCacheLoadingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
