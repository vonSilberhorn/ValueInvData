package com.szilberhornz.valueinvdata.services.stockvaluation.cache;

import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract class to provide extendibility in case the original caching solution needs to be replaced.
 */
public abstract class ValuationServerCache {

    private static final Logger LOG = LoggerFactory.getLogger(ValuationServerCache.class);

    //ConcurrentHashMap is thread safe but only locks parts of the map, so it is faster than the Collections.synchronizedMap
    protected final Map<String, RecordHolder> valuationServerCache = new ConcurrentHashMap<>();


    public abstract RecordHolder get(String ticker);

    public void put(final String ticker, final DiscountedCashFlowDTO dcfDto) {
        if (dcfDto != null) {
            if (!this.valuationServerCache.containsKey(ticker)) {
                final RecordHolder recordHolder = new RecordHolder(ticker);
                recordHolder.setDiscountedCashFlowDto(dcfDto);
                this.valuationServerCache.put(ticker, recordHolder);
            } else if (this.valuationServerCache.get(ticker).getDiscountedCashFlowDto() == null) {
                this.valuationServerCache.get(ticker).setDiscountedCashFlowDto(dcfDto);
            }
        } else {
            LOG.warn("Tried to add null DiscountedCashFlowDTO to cache for ticker {}", ticker);
        }
    }

    public void put(final String ticker, final PriceTargetConsensusDTO ptcDto) {
        if (ptcDto != null) {
            if (!this.valuationServerCache.containsKey(ticker)) {
                final RecordHolder recordHolder = new RecordHolder(ticker);
                recordHolder.setPriceTargetConsensusDto(ptcDto);
                this.valuationServerCache.put(ticker, recordHolder);
            } else if (this.valuationServerCache.get(ticker).getPriceTargetConsensusDto() == null) {
                this.valuationServerCache.get(ticker).setPriceTargetConsensusDto(ptcDto);
            }
        } else {
            LOG.warn("Tried to add null PriceTargetConsensusDTO to cache for ticker {}", ticker);
        }
    }

    public void put(final String ticker, final PriceTargetSummaryDTO ptsDto) {
        if (ptsDto != null) {
            if (!this.valuationServerCache.containsKey(ticker)) {
                final RecordHolder recordHolder = new RecordHolder(ticker);
                recordHolder.setPriceTargetSummaryDto(ptsDto);
                this.valuationServerCache.put(ticker, recordHolder);
            } else if (this.valuationServerCache.get(ticker).getPriceTargetSummaryDto() == null) {
                this.valuationServerCache.get(ticker).setPriceTargetSummaryDto(ptsDto);
            }
        } else {
            LOG.warn("Tried to add null PriceTargetSummaryDTO to cache for ticker {}", ticker);
        }
    }
}
