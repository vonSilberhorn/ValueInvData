package com.szilberhornz.valueinvdata.services.stockvaluation;

//System property based values passed to the JVM at startup
final class AppContext {

    static final int DEFAULT_SOCKET_BACKLOG = 32;

    private static final int DEFAULT_WORKER_THREAD_COUNT = 10;
    private static final String WORKER_THREADS_STRING = System.getProperty("worker_thread_count");
    static final int WORKER_THREADS =  WORKER_THREADS_STRING == null ? DEFAULT_WORKER_THREAD_COUNT : Integer.parseInt(WORKER_THREADS_STRING);

    private static final int DEFAULT_PORT = 8080;
    private static final String PORT_STRING = System.getProperty("port_number");
    static final int PORT_NUMBER = PORT_STRING == null ? DEFAULT_PORT : Integer.parseInt(PORT_STRING);


    private AppContext() {
        //no need to instantiate, only holds constant values
    }
}
