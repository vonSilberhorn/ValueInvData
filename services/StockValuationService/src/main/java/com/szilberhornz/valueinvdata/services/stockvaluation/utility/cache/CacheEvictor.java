package com.szilberhornz.valueinvdata.services.stockvaluation.utility.cache;

/**
 *  This interface can be used to run eviction jobs asynchronously
 */
@FunctionalInterface
public interface CacheEvictor {

    void runEviction();
}
