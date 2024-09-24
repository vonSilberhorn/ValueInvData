package com.szilberhornz.valueinvdata.services.stockvaluation.persistence.api;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.RecordHolder;

/**
 * The required behavior that all the underlying DB implementations need to provide
 */
public interface ValuationDBRepository {

    RecordHolder queryRecords(String ticker);

    void insertFullRecord(RecordHolder recordHolder);
}
