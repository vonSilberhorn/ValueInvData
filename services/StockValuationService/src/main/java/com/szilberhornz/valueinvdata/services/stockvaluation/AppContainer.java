package com.szilberhornz.valueinvdata.services.stockvaluation;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.SVSHttpHandler;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.fmp.FMPClient;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.fmp.authorization.FMPAuthorization;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.fmp.authorization.FMPAuthorizationImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;

class AppContainer {

    //stateless, immutable client, therefore it's also reusable
    final HttpClient httpClient = HttpClient.newHttpClient();

    final FMPAuthorization fmpAuthorization = new FMPAuthorizationImpl();
    //wrapper around the
    final FMPClient fmpClient = new FMPClient(fmpAuthorization, httpClient);

    HttpServer createHttpServer (int port) throws IOException {
        return HttpServer.create(new InetSocketAddress(port), AppContext.DEFAULT_SOCKET_BACKLOG);
    }

    HttpHandler createHandler(){
        return new SVSHttpHandler(fmpClient);
    }

}
