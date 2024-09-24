package com.szilberhornz.valueinvdata.services.stockvaluation.core;

import java.net.http.HttpClient;

public final class HttpClientFactory {

    public HttpClient newDefaultHttpClient(){
        return HttpClient
                .newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
}
