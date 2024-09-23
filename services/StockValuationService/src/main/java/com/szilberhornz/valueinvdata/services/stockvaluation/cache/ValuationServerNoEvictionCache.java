package com.szilberhornz.valueinvdata.services.stockvaluation.cache;

import org.jetbrains.annotations.Nullable;

public final class ValuationServerNoEvictionCache extends ValuationServerCache {


    @Override
    @Nullable
    public RecordHolder get(String ticker) {
        return this.valuationServerCache.get(ticker);
    }
}
