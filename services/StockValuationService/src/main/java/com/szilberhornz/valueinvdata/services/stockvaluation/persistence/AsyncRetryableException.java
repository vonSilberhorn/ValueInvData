package com.szilberhornz.valueinvdata.services.stockvaluation.persistence;

/**
 * Custom exception for the purpose of signaling retryability for the CompletableFuture instances
 * responsible for async db insertions
 */
public class AsyncRetryableException extends RuntimeException{

    public AsyncRetryableException(final Throwable cause) {
        super(cause);
    }
}
