package com.szilberhornz.valueinvdata.services.stockvaluation;

import org.jetbrains.annotations.Nullable;

//System property based values passed to the JVM at startup
public final class AppContext {

    static final int DEFAULT_SOCKET_BACKLOG = 32;

    private static final int DEFAULT_WORKER_THREAD_COUNT = 10;
    @Nullable
    private static final String WORKER_THREADS_STRING = System.getProperty("WORKER_THREAD_COUNT");
    static final int WORKER_THREADS =  WORKER_THREADS_STRING == null ? DEFAULT_WORKER_THREAD_COUNT : Integer.parseInt(WORKER_THREADS_STRING);

    private static final int DEFAULT_PORT = 8080;
    @Nullable
    private static final String PORT_STRING = System.getProperty("PORT_NUMBER");
    static final int PORT_NUMBER = PORT_STRING == null ? DEFAULT_PORT : Integer.parseInt(PORT_STRING);

    /**
     *  This property decides whether the app spins up an in-memory SQL db, or looks for a real MSSQL one.
     *  For demo purposes it's much simpler to use in-memory db, but of course this would be unthinkable in a
     *  production environment.
     *  <p/>
     *  For this very reason, the default is using in-memory-db (not setting the VM options results in false).
     *  If you want to use a real MSSQL instance, you should also pass the jdbc address of the db via the
     *  VM Option "MSSQL_JDBC_ADDRESS"
     */
    public static final boolean USE_MSSQL = Boolean.parseBoolean(System.getProperty("USE_MSSQL_DB"));

    @Nullable
    public static final String MSSQL_JDBC_ADDRESS = System.getProperty("MSSQL_JDBC_ADDRESS");

    private AppContext() {
        //no need to instantiate, only holds constant values
    }
}
