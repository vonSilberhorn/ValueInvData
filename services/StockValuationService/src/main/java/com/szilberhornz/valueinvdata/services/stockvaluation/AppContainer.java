package com.szilberhornz.valueinvdata.services.stockvaluation;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationServerCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationServerLFUCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationServerNoEvictionCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a simple inversion of control container, responsible for managing the class instances
 * of the application and providing dependency injection.
 * Note: this is a kind of functionality that frameworks like Spring provide out of the box.
 */
public final class AppContainer {

    private static final Logger LOG = LoggerFactory.getLogger(AppContainer.class);

    final ValuationServerCache cache = this.initializeCache();

    private ValuationServerCache initializeCache() {
        if (AppContext.IS_DEMO_MODE && !AppContext.USE_LFU_CACHE) {
            LOG.info("Starting a cache with no eviction policy!");
            return new ValuationServerNoEvictionCache();
        } else {
            LOG.info("Starting an LFU cache!");
            return new ValuationServerLFUCache(AppContext.LFU_REBALANCE_THRESHOLD, AppContext.LFU_CACHE_SIZE);
        }
    }

    public ValuationServerCache getCache() {
        return this.cache;
    }
}
