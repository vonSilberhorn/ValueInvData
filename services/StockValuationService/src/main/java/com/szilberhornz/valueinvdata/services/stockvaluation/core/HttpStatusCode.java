package com.szilberhornz.valueinvdata.services.stockvaluation.core;

public enum HttpStatusCode {

    OK(200),

    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    TOO_MANY_REQUESTS(429),

    INTERNAL_SERVER_ERROR(500);

    final int statusCodeNumber;

    HttpStatusCode(final int statusCodeNumber) {
        this.statusCodeNumber = statusCodeNumber;
    }

    public int getStatusCode(){
        return this.statusCodeNumber;
    }
}
