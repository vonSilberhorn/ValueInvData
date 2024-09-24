package com.szilberhornz.valueinvdata.services.stockvaluation.cache;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * This is a constant size cache for all the existing tickers, used to verify user-input.
 * This is a mission-critical class, failing to load a cache means that any caller can swarm the application
 * with invalid requests, depleting the rate limits of the FMP api key for absolutely no meaningful benefits.
 * So if this class, cannot initialize, the application should exit instead of skipping over the issue.
 */
public final class TickerCache {

    private static final Set<String> TICKER_CACHE = new HashSet<>();

    private final String resourceFileName;

    public TickerCache(String resourceFileName) {
        this.resourceFileName = resourceFileName;
        this.loadCache();
    }

    public boolean tickerExists(final String ticker){
        return TICKER_CACHE.contains(ticker.toUpperCase());
    }

    //this currentThread() in the method should always be the [main] since initialization happens in the AppContainer, before
    //the app starts the http server
    private void loadCache(){
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(this.resourceFileName)){
            if (in != null) {
                final Scanner scanner = new Scanner(in, StandardCharsets.UTF_8).useDelimiter(",");
                while (scanner.hasNext()) {
                    final String next = scanner.next();
                    //we have to remove leading and trailing quotes
                    TICKER_CACHE.add(next.substring(1, next.length()-1));
                }
            } else {
                throw new TickerCacheLoadingFailedException("Ticker cache loading failed because the resource file " + this.resourceFileName + " does not exist or is empty!");
            }
        } catch (final IOException ioe){
            throw new TickerCacheLoadingFailedException("Ticker cache loading failed due to the following reason: ", ioe);
        }
    }
}
