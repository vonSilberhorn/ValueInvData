package com.szilberhornz.valueinvdata.services.stockvaluation.persistence.inmem;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationServerCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.persistence.api.ValuationDBRepository;


public class InMemoryValuationDb implements ValuationDBRepository {


    @Override
    public DiscountedCashFlowDTO getDiscountedCashFlowReport(String ticker) {
        return null;
    }

    @Override
    public PriceTargetSummaryDTO getPriceTargetSummaryReport(String ticker) {
        return null;
    }

    @Override
    public PriceTargetConsensusDTO getPriceTargetConsensusReport(String ticker) {
        return null;
    }

    @Override
    public void insertFullRecord(ValuationServerCache.RecordHolder recordHolder) {

    }
}
