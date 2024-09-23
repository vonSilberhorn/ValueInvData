package com.szilberhornz.valueinvdata.services.stockvaluation.cache;

/**
 *  This interface can be used to run eviction jobs asynchronously
 */
@FunctionalInterface
public interface CacheEvictor {

    void runEviction();
}
