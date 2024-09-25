package com.szilberhornz.valueinvdata.services.stockvaluation.persistence;

public class AsyncRetryableException extends RuntimeException{

    public AsyncRetryableException(final Throwable cause) {
        super(cause);
    }
}
