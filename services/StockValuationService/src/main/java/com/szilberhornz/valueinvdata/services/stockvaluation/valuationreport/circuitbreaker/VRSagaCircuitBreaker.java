package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.circuitbreaker;

public interface VRSagaCircuitBreaker {

    long getTimeoutForApiCallInMillis();
    long getTimeoutForDbQueryInMillis();
    long getOverallTimeoutInMillis();
}
