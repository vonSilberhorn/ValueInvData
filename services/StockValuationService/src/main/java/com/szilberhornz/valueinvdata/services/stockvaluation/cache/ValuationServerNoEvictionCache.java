package com.szilberhornz.valueinvdata.services.stockvaluation.cache;

import org.jetbrains.annotations.Nullable;

/**
 * No eviction implementation of the ValuationServerCache. Should only be used in demo mode!
 */
public final class ValuationServerNoEvictionCache extends ValuationServerCache {

    @Override
    @Nullable
    public RecordHolder get(final String ticker) {
        return this.valuationServerCache.get(ticker);
    }
}
