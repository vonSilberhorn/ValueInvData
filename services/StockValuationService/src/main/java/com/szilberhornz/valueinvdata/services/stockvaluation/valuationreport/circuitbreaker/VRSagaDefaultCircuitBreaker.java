package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.circuitbreaker;

/**
 * A very simple circuit breaker to make sure requests don't last forever, and we return all the data we
 * can
 */
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
