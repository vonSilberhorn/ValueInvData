package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.circuitbreaker;

/**
 * A very simple circuit breaker to make sure requests don't last forever, and we return all the data we
 * can
 */
public class VRSagaDefaultCircuitBreaker implements VRSagaCircuitBreaker {

    //private static final long DEFAULT_TIMEOUT_FOR_API_CALL_IN_MILLIS = 2500;
    private static final long DEFAULT_TIMEOUT_FOR_API_CALL_IN_MILLIS = Long.MAX_VALUE;
   // private static final long DEFAULT_TIMEOUT_FOR_DB_QUERY_IN_MILLIS = 2000;
    private static final long DEFAULT_TIMEOUT_FOR_DB_QUERY_IN_MILLIS = Long.MAX_VALUE;
    //private static final long DEFAULT_OVERALL_TIMEOUT_IN_MILLIS = 5000;
    private static final long DEFAULT_OVERALL_TIMEOUT_IN_MILLIS = Long.MAX_VALUE;

    @Override
    public long getTimeoutForApiCallInMillis() {
        return DEFAULT_TIMEOUT_FOR_API_CALL_IN_MILLIS;
    }

    @Override
    public long getTimeoutForDbQueryInMillis() {
        return DEFAULT_TIMEOUT_FOR_DB_QUERY_IN_MILLIS;
    }

    @Override
    public long getOverallTimeoutInMillis() {
        return DEFAULT_OVERALL_TIMEOUT_IN_MILLIS;
    }
}
