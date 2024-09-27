package com.szilberhornz.valueinvdata.services.stockvaluation;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class StockValuationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StockValuationService.class);


    public static void main(final String[] args) throws IOException {
        final String serviceName = StockValuationService.class.getSimpleName();
        LOGGER.info("Hello world! This is a new demo service called {}", serviceName);
        //pick up possible user settings via System properties
        final int listeningPort = AppContext.PORT_NUMBER;
        final int workerThreads = AppContext.WORKER_THREADS;
        //create the java objects for the app
        final AppContainer container = new AppContainer();
        final HttpServer httpServer = container.createHttpServer(listeningPort);
        final HttpHandler requestHandler = container.getHttpHandler();
        //let's go
        httpServer.createContext("/", requestHandler);
        final Executor executor = Executors.newFixedThreadPool(workerThreads);
        httpServer.setExecutor(executor);
        httpServer.start();
        LOGGER.info("{} http server started on port: {} with {} worker threads!", serviceName, listeningPort, workerThreads);
    }
}