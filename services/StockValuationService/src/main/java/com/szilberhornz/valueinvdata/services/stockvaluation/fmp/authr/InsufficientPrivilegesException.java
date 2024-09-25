package com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr;

public class InsufficientPrivilegesException extends ApiKeyException {

    public InsufficientPrivilegesException(final String message) {
        super(message);
    }
}
