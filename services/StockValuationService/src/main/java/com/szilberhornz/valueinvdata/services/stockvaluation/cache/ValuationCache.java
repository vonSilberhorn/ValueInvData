package com.szilberhornz.valueinvdata.services.stockvaluation.cache;

import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.record.DiscountedCashFlowDTO;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ValuationCache {

    //ConcurrentHashMap is thread safe but only locks parts of the map, so it is faster than the Collections.synchronizedMap
    private final Map<String, DiscountedCashFlowDTO> tickerToDcfCache = new ConcurrentHashMap<>();

    public DiscountedCashFlowDTO getDcfFromCache(final String ticker) {
        return this.tickerToDcfCache.get(ticker);
    }

    public void putIntoDcfCache(final String ticker, final DiscountedCashFlowDTO dcfDto) {
        this.tickerToDcfCache.put(ticker, dcfDto);
    }
}
