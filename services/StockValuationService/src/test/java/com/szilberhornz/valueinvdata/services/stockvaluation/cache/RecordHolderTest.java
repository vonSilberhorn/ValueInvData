package com.szilberhornz.valueinvdata.services.stockvaluation.cache;

import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecordHolderTest {

    private final DiscountedCashFlowDTO dcfDto = new DiscountedCashFlowDTO("DUMMY", "2024-09-26", 15.5, 14);
    private final PriceTargetConsensusDTO ptcDto = new PriceTargetConsensusDTO("DUMMY", 20, 10, 16, 15);
    private final PriceTargetSummaryDTO ptsDto = new PriceTargetSummaryDTO("DUMMY", 2, 16, 5, 14);

    @Test
    void testFullRecord(){
        final RecordHolder record = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, this.ptcDto, this.ptsDto);
        assertEquals(3, record.getDtoCount());
        assertFalse(record.isDataMissing());
        assertNull(record.getCauseOfNullDtos());
        assertEquals("DUMMY", record.getTicker());
    }

    @Test
    void testPartialRecord(){
        final RecordHolder record = RecordHolder.newRecordHolder("DUMMY", null, this.ptcDto, this.ptsDto);
        assertEquals(2, record.getDtoCount());
        assertTrue(record.isDataMissing());
        assertNull(record.getCauseOfNullDtos());
        assertEquals("DUMMY", record.getTicker());
    }

    @Test
    void testPartialRecord2(){
        final RecordHolder record = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, null, this.ptsDto);
        assertEquals(2, record.getDtoCount());
        assertTrue(record.isDataMissing());
        assertNull(record.getCauseOfNullDtos());
        assertEquals("DUMMY", record.getTicker());
    }

    @Test
    void testPartialRecord3(){
        final RecordHolder record = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, this.ptcDto, null);
        assertEquals(2, record.getDtoCount());
        assertTrue(record.isDataMissing());
        assertNull(record.getCauseOfNullDtos());
        assertEquals("DUMMY", record.getTicker());
    }
}
