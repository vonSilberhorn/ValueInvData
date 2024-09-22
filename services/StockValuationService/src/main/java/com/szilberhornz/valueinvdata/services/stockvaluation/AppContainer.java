package com.szilberhornz.valueinvdata.services.stockvaluation;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.SVSHttpHandler;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.FMPClient;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authorization.FMPAuthorization;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authorization.FMPAuthorizationImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;

class AppContainer {

    //stateless, immutable client, therefore it's also reusable
    final HttpClient httpClient = HttpClient.newHttpClient();

    final FMPAuthorization fmpAuthorization = new FMPAuthorizationImpl();
    final ValuationCache valuationCache = new ValuationCache();
    //wrapper around the
    final FMPClient fmpClient = new FMPClient(fmpAuthorization, httpClient, valuationCache);

    HttpServer createHttpServer (int port) throws IOException {
        return HttpServer.create(new InetSocketAddress(port), AppContext.DEFAULT_SOCKET_BACKLOG);
    }

    HttpHandler createHandler(){
        return new SVSHttpHandler(fmpClient);
    }

}
