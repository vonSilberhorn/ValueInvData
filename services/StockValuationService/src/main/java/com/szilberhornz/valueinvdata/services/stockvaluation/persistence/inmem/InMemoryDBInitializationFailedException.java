package com.szilberhornz.valueinvdata.services.stockvaluation.persistence.inmem;

public class InMemoryDBInitializationFailedException extends RuntimeException {

    public InMemoryDBInitializationFailedException(String message) {
        super(message);
    }

    public InMemoryDBInitializationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
