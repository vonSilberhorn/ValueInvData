package com.szilberhornz.valueinvdata.services.stockvaluation;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationServerCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationServerLFUCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationServerNoEvictionCache;

/**
 * This class is a simple inversion of control container, responsible for managing the class instances
 * of the application and providing dependency injection.
 * Note: this is a kind of functionality that frameworks like Spring provide out of the box.
 */
public final class AppContainer {

    final ValuationServerCache cache = AppContext.IS_DEMO_MODE
            ? new ValuationServerNoEvictionCache()
            : new ValuationServerLFUCache(AppContext.LFU_REBALANCE_THRESHOLD);
}
