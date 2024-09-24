package com.szilberhornz.valueinvdata.services.stockvaluation.persistence.api;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationServerCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;

/**
 * The required behavior that all the underlying DB implementations need to provide
 */
public interface ValuationDBRepository {

    DiscountedCashFlowDTO getDiscountedCashFlowReport(String ticker);
    PriceTargetSummaryDTO getPriceTargetSummaryReport(String ticker);
    PriceTargetConsensusDTO getPriceTargetConsensusReport(String ticker);

    void insertFullRecord(ValuationServerCache.RecordHolder recordHolder);
}
