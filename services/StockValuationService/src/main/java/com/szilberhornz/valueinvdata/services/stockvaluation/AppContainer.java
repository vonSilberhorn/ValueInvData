package com.szilberhornz.valueinvdata.services.stockvaluation;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.szilberhornz.valueinvdata.services.stockvaluation.cache.TickerCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationServerCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationServerLFUCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationServerNoEvictionCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.HttpClientFactory;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.StockValuationServiceHttpHandler;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.FMPApiHttpClient;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.FMPResponseHandler;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr.FMPAuthorizer;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr.JVMBasedFMPAuthorizer;
import com.szilberhornz.valueinvdata.services.stockvaluation.persistence.ValuationDBRepositoryImpl;
import com.szilberhornz.valueinvdata.services.stockvaluation.persistence.api.ValuationDBRepository;
import com.szilberhornz.valueinvdata.services.stockvaluation.persistence.inmem.InMemoryDBDataSourceFactory;
import com.szilberhornz.valueinvdata.services.stockvaluation.persistence.mssql.MSSQLDataSourceFactory;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.VRSagaDataBroker;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.VRSagaOrchestrator;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.ValuationReportRestController;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.circuitbreaker.VRSagaCircuitBreaker;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.circuitbreaker.VRSagaDefaultCircuitBreaker;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.formatter.ValuationResponseBodyExplainerFormatter;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.formatter.ValuationResponseBodyFormatter;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.formatter.ValuationResponseBodyJSONFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * This class is a simple inversion of control container, responsible for managing the class instances
 * of the application and providing dependency injection.
 * Note: this is a kind of functionality that frameworks like Spring provide out of the box.
 */
public final class AppContainer {

    private static final Logger LOG = LoggerFactory.getLogger(AppContainer.class);

    final ValuationServerCache cache = this.initializeCache();
    final DataSource dataSource = this.initializeDataSource();
    final ValuationDBRepository valuationDBRepository = new ValuationDBRepositoryImpl(this.dataSource);

    final HttpClientFactory httpClientFactory = new HttpClientFactory();
    final FMPAuthorizer fmpAuthorizer = new JVMBasedFMPAuthorizer();
    final FMPApiHttpClient fmpApiHttpClient = new FMPApiHttpClient(this.fmpAuthorizer, this.httpClientFactory);
    final FMPResponseHandler fmpResponseHandler = new FMPResponseHandler(this.fmpApiHttpClient);
    final VRSagaDataBroker vrSagaDataBroker = new VRSagaDataBroker(this.valuationDBRepository, this.cache, this.fmpResponseHandler);
    final TickerCache tickerCache = new TickerCache("tickers.txt");
    final VRSagaCircuitBreaker vrSagaCircuitBreaker = new VRSagaDefaultCircuitBreaker();
    final ValuationResponseBodyFormatter formatter = this.getFormatter();

    final VRSagaOrchestrator vrSagaOrchestrator = new VRSagaOrchestrator(this.tickerCache, this.formatter, this.vrSagaCircuitBreaker, this.vrSagaDataBroker);

    final ValuationReportRestController valuationReportRestController = new ValuationReportRestController(this.vrSagaOrchestrator);

    final StockValuationServiceHttpHandler httpHandler = new StockValuationServiceHttpHandler(this.valuationReportRestController);

    HttpHandler getHttpHandler(){
        return this.httpHandler;
    }

    HttpServer createHttpServer (final int port) throws IOException {
        return HttpServer.create(new InetSocketAddress(port), AppContext.DEFAULT_SOCKET_BACKLOG);
    }

    private ValuationServerCache initializeCache() {
        if (AppContext.IS_DEMO_MODE && !AppContext.USE_LFU_CACHE) {
            LOG.info("Starting a cache with no eviction policy!");
            return new ValuationServerNoEvictionCache();
        } else {
            LOG.info("Starting an LFU cache!");
            return new ValuationServerLFUCache(AppContext.LFU_REBALANCE_THRESHOLD, AppContext.LFU_CACHE_SIZE);
        }
    }

    private ValuationResponseBodyFormatter getFormatter(){
        if (!AppContext.IS_DEMO_MODE) {
            return new ValuationResponseBodyJSONFormatter();
        } else if ("JSON".equalsIgnoreCase(AppContext.VALUATION_REPORT_FORMAT)) {
            return new ValuationResponseBodyJSONFormatter();
        } else {
            return new ValuationResponseBodyExplainerFormatter();
        }
    }

    private DataSource initializeDataSource(){
        if (AppContext.MSSQL_ADDRESS != null) {
            return new MSSQLDataSourceFactory().getValuationDbDataSource();
        } else {
            return new InMemoryDBDataSourceFactory("inMemDbInitializer.txt").getValuationDbDataSource();
        }
    }

    public ValuationServerCache getCache() {
        return this.cache;
    }
}
