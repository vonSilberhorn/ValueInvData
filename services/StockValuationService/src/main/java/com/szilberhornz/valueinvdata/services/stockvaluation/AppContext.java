package com.szilberhornz.valueinvdata.services.stockvaluation;

import java.util.Set;

/**
 * This class is a collection of static variables, some of which are coming from VMOptions.
 * I chose not to use any files to hold AppContext values because a) it's a small app,
 * b) even though it's not a concern, file changes need releases while command line changes
 * only need a bounce to take effect.
 */
public final class AppContext {

    public static final boolean IS_DEMO_MODE = !"PROD".equalsIgnoreCase(System.getProperty("APP_MODE"));

    static final int DEFAULT_SOCKET_BACKLOG = 32;
    private static final int DEFAULT_WORKER_THREAD_COUNT = 10;
    private static final String WORKER_THREADS_STRING = System.getProperty("WORKER_THREAD_COUNT");
    static final int WORKER_THREADS =  WORKER_THREADS_STRING == null ? DEFAULT_WORKER_THREAD_COUNT : Integer.parseInt(WORKER_THREADS_STRING);
    private static final int DEFAULT_PORT = 8080;
    private static final String PORT_STRING = System.getProperty("PORT_NUMBER");
    static final int PORT_NUMBER = PORT_STRING == null ? DEFAULT_PORT : Integer.parseInt(PORT_STRING);

    //cache related stuff
    private static final int DEFAULT_LFU_CACHE_SIZE = 2000;
    private static final int DEFAULT_LFU_REBALANCE_THRESHOLD = 200;
    private static final String LFU_CACHE_STRING = System.getProperty("LFU_CACHE_SIZE");
    private static final String LFU_REBALANCE_STRING = System.getProperty("LFU_REBALANCE_THRESHOLD");
    static final boolean USE_LFU_CACHE = Boolean.parseBoolean(System.getProperty("USE_LFU_CACHE"));
    static final int LFU_CACHE_SIZE = LFU_CACHE_STRING == null ? DEFAULT_LFU_CACHE_SIZE : Integer.parseInt(LFU_CACHE_STRING);
    static final int LFU_REBALANCE_THRESHOLD = LFU_REBALANCE_STRING == null ? DEFAULT_LFU_REBALANCE_THRESHOLD : Integer.parseInt(LFU_REBALANCE_STRING);

    public static final Set<Integer> RETRYABLE_HTTP_STATUS_CODES = Set.of(408, 502, 503, 504);

    public static final String MSSQL_ADDRESS = System.getProperty("MSSQL_ADDRESS");
    public static final String VALUATION_REPORT_FORMAT = System.getProperty("VALUATION_REPORT_FORMAT");


    private AppContext() {
        //hide implicit public constructor. This class will only contain constants so no need to instantiate
    }
}
