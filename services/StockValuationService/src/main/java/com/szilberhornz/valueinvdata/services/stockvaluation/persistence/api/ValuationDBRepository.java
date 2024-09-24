package com.szilberhornz.valueinvdata.services.stockvaluation.persistence.api;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationServerCache;

/**
 * The required behavior that all the underlying DB implementations need to provide
 */
public interface ValuationDBRepository {

    ValuationServerCache.RecordHolder queryRecords(String ticker);

    void insertFullRecord(ValuationServerCache.RecordHolder recordHolder);
}
