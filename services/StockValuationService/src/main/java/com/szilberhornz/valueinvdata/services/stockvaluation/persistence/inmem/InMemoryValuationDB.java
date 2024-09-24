package com.szilberhornz.valueinvdata.services.stockvaluation.persistence.inmem;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationServerCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.persistence.api.ValuationDBRepository;


public class InMemoryValuationDB implements ValuationDBRepository {

    @Override
    public ValuationServerCache.RecordHolder queryRecords(String ticker) {
        return null;
    }

    @Override
    public void insertFullRecord(ValuationServerCache.RecordHolder recordHolder) {

    }
}
