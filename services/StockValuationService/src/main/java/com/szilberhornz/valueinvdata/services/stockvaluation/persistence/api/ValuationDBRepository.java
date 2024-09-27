package com.szilberhornz.valueinvdata.services.stockvaluation.persistence.api;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.RecordHolder;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;

/**
 * The required behavior that all the underlying DB implementations need to provide
 */
public interface ValuationDBRepository {

    RecordHolder queryRecords(String ticker);

    DiscountedCashFlowDTO queryDiscountedCashFlowData(String ticker);

    PriceTargetSummaryDTO queryPriceTargetSummaryData(String ticker);

    PriceTargetConsensusDTO queryPriceTargetConsensusData(String ticker);

    void insertFullRecord(RecordHolder recordHolder);

    void insertDiscountedCashFlowData(DiscountedCashFlowDTO discountedCashFlowDTO);

    void insertPriceTargetSummaryData(PriceTargetSummaryDTO priceTargetSummaryDTO);

    void insertPriceTargetConsensusData(PriceTargetConsensusDTO priceTargetConsensusDTO);
}
