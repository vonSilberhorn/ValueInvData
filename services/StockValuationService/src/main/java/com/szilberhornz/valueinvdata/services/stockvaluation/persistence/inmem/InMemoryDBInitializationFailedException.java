package com.szilberhornz.valueinvdata.services.stockvaluation.persistence.inmem;

/**
 * In-memory db initialization failure means the app is in illegal state, as the required file to
 * do the successful initialization is packaged into the app already, so where did it go?
 */
public class InMemoryDBInitializationFailedException extends IllegalStateException {

    public InMemoryDBInitializationFailedException(String message) {
        super(message);
    }

    public InMemoryDBInitializationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
