package com.szilberhornz.valueinvdata.services.stockvaluation.fmp;

public class RateLimitReachedException extends Exception {

    public RateLimitReachedException(String message) {
        super(message);
    }
}
