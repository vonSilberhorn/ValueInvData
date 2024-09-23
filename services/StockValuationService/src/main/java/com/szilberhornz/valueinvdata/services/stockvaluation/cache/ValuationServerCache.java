package com.szilberhornz.valueinvdata.services.stockvaluation.cache;

import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.record.PriceTargetSummaryDTO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract class to provide extendibility in case the original caching solution need to be replaced.
 */
public abstract class ValuationServerCache {

    //ConcurrentHashMap is thread safe but only locks parts of the map, so it is faster than the Collections.synchronizedMap
    protected final Map<String, RecordHolder> valuationServerCache = new ConcurrentHashMap<>();

    public abstract RecordHolder get(String ticker);

    public void put(String ticker, DiscountedCashFlowDTO dcfDto) {
        if (!this.valuationServerCache.containsKey(ticker)) {
            final RecordHolder recordHolder = new RecordHolder(ticker);
            recordHolder.setDiscountedCashFlowDto(dcfDto);
            this.valuationServerCache.put(ticker, recordHolder);
        } else if (this.valuationServerCache.get(ticker).getDiscountedCashFlowDto() == null) {
            this.valuationServerCache.get(ticker).setDiscountedCashFlowDto(dcfDto);
        }
    }

    public void put(String ticker, PriceTargetConsensusDTO ptcDto) {
        if (!this.valuationServerCache.containsKey(ticker)) {
            final RecordHolder recordHolder = new RecordHolder(ticker);
            recordHolder.setPriceTargetConsensusDto(ptcDto);
            this.valuationServerCache.put(ticker, recordHolder);
        } else if (this.valuationServerCache.get(ticker).getDiscountedCashFlowDto() == null) {
            this.valuationServerCache.get(ticker).setPriceTargetConsensusDto(ptcDto);
        }
    }

    public void put(String ticker, PriceTargetSummaryDTO ptsDto) {
        if (!this.valuationServerCache.containsKey(ticker)) {
            final RecordHolder recordHolder = new RecordHolder(ticker);
            recordHolder.setPriceTargetSummaryDto(ptsDto);
            this.valuationServerCache.put(ticker, recordHolder);
        } else if (this.valuationServerCache.get(ticker).getDiscountedCashFlowDto() == null) {
            this.valuationServerCache.get(ticker).setPriceTargetSummaryDto(ptsDto);
        }
    }

    /**
     *   This is a static nested class to prevent outside entities to change the state of its instances: e.g. a class looking up
     *   cached items should be able to see what is inside the item, but changes should only be made within the
     *   {@link ValuationServerCache} class or its children, to ensure the consistency of the cache itself.
     *   <p/>
     *   No need to synchronize this class due to the fact that the ConcurrentHashMap locks the bucket when performing write operations
     */
    public static class RecordHolder {

        private final String ticker;

        private DiscountedCashFlowDTO discountedCashFlowDto;
        private PriceTargetConsensusDTO priceTargetConsensusDto;
        private PriceTargetSummaryDTO priceTargetSummaryDto;

        //we don't allow anyone to construct this object outside the actual cache implementors
        protected RecordHolder(String ticker) {
            this.ticker = ticker;
        }

        @NotNull
        public String getTicker() {
            return ticker;
        }

        @Nullable
        public DiscountedCashFlowDTO getDiscountedCashFlowDto() {
            return discountedCashFlowDto;
        }


        //we don't allow anyone to access setters outside the actual cache implementors
        protected void setDiscountedCashFlowDto(DiscountedCashFlowDTO discountedCashFlowDto) {
            this.discountedCashFlowDto = discountedCashFlowDto;
        }

        @Nullable
        public PriceTargetConsensusDTO getPriceTargetConsensusDto() {
            return priceTargetConsensusDto;
        }

        //we don't allow anyone to access setters outside the actual cache implementors
        protected void setPriceTargetConsensusDto(PriceTargetConsensusDTO priceTargetConsensusDto) {
            this.priceTargetConsensusDto = priceTargetConsensusDto;
        }

        @Nullable
        public PriceTargetSummaryDTO getPriceTargetSummaryDto() {
            return priceTargetSummaryDto;
        }

        //we don't allow anyone to access setters outside the actual cache implementors
        protected void setPriceTargetSummaryDto(PriceTargetSummaryDTO priceTargetSummaryDto) {
            this.priceTargetSummaryDto = priceTargetSummaryDto;
        }
    }
}
