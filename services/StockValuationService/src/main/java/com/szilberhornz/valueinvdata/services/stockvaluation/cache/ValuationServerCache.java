package com.szilberhornz.valueinvdata.services.stockvaluation.cache;

import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract class to provide extendibility in case the original caching solution need to be replaced.
 */
public abstract class ValuationServerCache {

    //ConcurrentHashMap is thread safe but only locks parts of the map, so it is faster than the Collections.synchronizedMap
    protected final Map<String, RecordHolder> valuationServerCache = new ConcurrentHashMap<>();

    public abstract RecordHolder get(String ticker);

    public void put(final String ticker, final DiscountedCashFlowDTO dcfDto) {
        if (!this.valuationServerCache.containsKey(ticker)) {
            final RecordHolder recordHolder = new RecordHolder(ticker);
            recordHolder.setDiscountedCashFlowDto(dcfDto);
            this.valuationServerCache.put(ticker, recordHolder);
        } else if (this.valuationServerCache.get(ticker).getDiscountedCashFlowDto() == null) {
            this.valuationServerCache.get(ticker).setDiscountedCashFlowDto(dcfDto);
        }
    }

    public void put(final String ticker, final PriceTargetConsensusDTO ptcDto) {
        if (!this.valuationServerCache.containsKey(ticker)) {
            final RecordHolder recordHolder = new RecordHolder(ticker);
            recordHolder.setPriceTargetConsensusDto(ptcDto);
            this.valuationServerCache.put(ticker, recordHolder);
        } else if (this.valuationServerCache.get(ticker).getDiscountedCashFlowDto() == null) {
            this.valuationServerCache.get(ticker).setPriceTargetConsensusDto(ptcDto);
        }
    }

    public void put(final String ticker, final PriceTargetSummaryDTO ptsDto) {
        if (!this.valuationServerCache.containsKey(ticker)) {
            final RecordHolder recordHolder = new RecordHolder(ticker);
            recordHolder.setPriceTargetSummaryDto(ptsDto);
            this.valuationServerCache.put(ticker, recordHolder);
        } else if (this.valuationServerCache.get(ticker).getDiscountedCashFlowDto() == null) {
            this.valuationServerCache.get(ticker).setPriceTargetSummaryDto(ptsDto);
        }
    }
}
