package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.fmp;

public class RateLimitReachedException extends RuntimeException {

    public RateLimitReachedException(final String message) {
        super(message);
    }
}
