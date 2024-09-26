package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.circuitbreaker;

public class VRSagaDefaultCircuitBreaker implements VRSagaCircuitBreaker {

    final long timeoutForApiCallInMillis = 2500;
    final long timeoutForDbQueryInMillis = 2000;
    final long overallTimeoutInMillis = 5000;

    @Override
    public long getTimeoutForApiCallInMillis() {
        return this.timeoutForApiCallInMillis;
    }

    @Override
    public long getTimeoutForDbQueryInMillis() {
        return this.timeoutForDbQueryInMillis;
    }

    @Override
    public long getOverallTimeoutInMillis() {
        return this.overallTimeoutInMillis;
    }
}
