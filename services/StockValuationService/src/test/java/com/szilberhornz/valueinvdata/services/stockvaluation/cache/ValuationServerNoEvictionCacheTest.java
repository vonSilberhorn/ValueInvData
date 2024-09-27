package com.szilberhornz.valueinvdata.services.stockvaluation.cache;

import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ValuationServerNoEvictionCacheTest {

    private final ValuationServerNoEvictionCache sut = new ValuationServerNoEvictionCache();

    @Test
    void putAndGetDcfDtoWhenNoCachedItemExists(){
        final DiscountedCashFlowDTO dcfDto = new DiscountedCashFlowDTO("AAPL", "2024-09-24", 189.22, 220.2);
        this.sut.put(dcfDto.ticker(), dcfDto);
        assertEquals(189.22, this.sut.get("AAPL").getDiscountedCashFlowDto().dcf());
        assertNull(this.sut.get("AAPL").getPriceTargetConsensusDto());
        assertNull(this.sut.get("AAPL").getPriceTargetSummaryDto());
    }

    @Test
    void putAndGetDcfDtoWhenRecordHolderExists(){
        final PriceTargetSummaryDTO ptsDto = new PriceTargetSummaryDTO("AAPL", 5, 213.23, 14, 201.12);
        this.sut.put(ptsDto.ticker(), ptsDto);
        final DiscountedCashFlowDTO dcfDto = new DiscountedCashFlowDTO("AAPL", "2024-09-24", 189.22, 220.2);
        this.sut.put(dcfDto.ticker(), dcfDto);
        assertEquals(189.22, this.sut.get("AAPL").getDiscountedCashFlowDto().dcf());
        assertEquals(213.23, this.sut.get("AAPL").getPriceTargetSummaryDto().lastMonthAvgPriceTarget());
        assertNull(this.sut.get("AAPL").getPriceTargetConsensusDto());
    }

    @Test
    void putAndGetPtsDtoWhenNoCachedItemExists(){
        final PriceTargetSummaryDTO ptsDto = new PriceTargetSummaryDTO("AAPL", 5, 213.23, 14, 201.12);
        this.sut.put(ptsDto.ticker(), ptsDto);
        assertEquals(213.23, this.sut.get("AAPL").getPriceTargetSummaryDto().lastMonthAvgPriceTarget());
        assertNull(this.sut.get("AAPL").getPriceTargetConsensusDto());
        assertNull(this.sut.get("AAPL").getDiscountedCashFlowDto());
    }

    @Test
    void putAndGetPtsDtoWhenRecordHolderExists(){
        final PriceTargetConsensusDTO otcDto = new PriceTargetConsensusDTO("AAPL", 240, 176, 220.2, 210);
        this.sut.put(otcDto.ticker(), otcDto);
        final PriceTargetSummaryDTO ptsDto = new PriceTargetSummaryDTO("AAPL", 5, 213.23, 14, 201.12);
        this.sut.put(ptsDto.ticker(), ptsDto);
        assertEquals(176, this.sut.get("AAPL").getPriceTargetConsensusDto().targetLow());
        assertEquals(213.23, this.sut.get("AAPL").getPriceTargetSummaryDto().lastMonthAvgPriceTarget());
        assertNull(this.sut.get("AAPL").getDiscountedCashFlowDto());
    }

    @Test
    void putAndGetPtcDtoWhenNoCachedItemExists(){
        final PriceTargetConsensusDTO otcDto = new PriceTargetConsensusDTO("AAPL", 240, 176, 220.2, 210);
        this.sut.put(otcDto.ticker(), otcDto);
        assertEquals(240, this.sut.get("AAPL").getPriceTargetConsensusDto().targetHigh());
        assertNull(this.sut.get("AAPL").getDiscountedCashFlowDto());
        assertNull(this.sut.get("AAPL").getPriceTargetSummaryDto());
    }

    @Test
    void putAndGetPtcDtoWhenRecordHolderExists(){
        final PriceTargetSummaryDTO ptsDto = new PriceTargetSummaryDTO("AAPL", 5, 213.23, 14, 201.12);
        this.sut.put(ptsDto.ticker(), ptsDto);
        final PriceTargetConsensusDTO otcDto = new PriceTargetConsensusDTO("AAPL", 240, 176, 220.2, 210);
        this.sut.put(otcDto.ticker(), otcDto);
        assertEquals(176, this.sut.get("AAPL").getPriceTargetConsensusDto().targetLow());
        assertEquals(213.23, this.sut.get("AAPL").getPriceTargetSummaryDto().lastMonthAvgPriceTarget());
        assertNull(this.sut.get("AAPL").getDiscountedCashFlowDto());
    }

    @Test
    void putNullPtsDtoShouldDoNothing() {
        final PriceTargetSummaryDTO ptsDto = null;
        this.sut.put("DUMMY", ptsDto);
        assertNull(this.sut.get("DUMMY"));
    }

    @Test
    void putNullPtcDtoShouldDoNothing() {
        final PriceTargetConsensusDTO ptcDto = null;
        this.sut.put("DUMMY", ptcDto);
        assertNull(this.sut.get("DUMMY"));
    }

    @Test
    void putNullDcfDtoShouldDoNothing() {
        final DiscountedCashFlowDTO dcfDto = null;
        this.sut.put("DUMMY", dcfDto);
        assertNull(this.sut.get("DUMMY"));
    }
}
