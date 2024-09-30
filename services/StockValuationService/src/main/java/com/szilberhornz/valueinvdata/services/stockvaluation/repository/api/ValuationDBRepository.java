package com.szilberhornz.valueinvdata.services.stockvaluation.repository.api;

import com.szilberhornz.valueinvdata.services.stockvaluation.utility.cache.RecordHolder;
import com.szilberhornz.valueinvdata.services.stockvaluation.model.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.model.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.model.record.PriceTargetSummaryDTO;

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
