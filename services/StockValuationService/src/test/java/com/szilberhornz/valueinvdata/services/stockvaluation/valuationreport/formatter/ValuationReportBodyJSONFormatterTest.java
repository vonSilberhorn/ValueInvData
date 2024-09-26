package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.formatter;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.RecordHolder;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValuationReportBodyJSONFormatterTest {

    private final ValuationResponseBodyJSONFormatter sut = new ValuationResponseBodyJSONFormatter();

    private final DiscountedCashFlowDTO dcfDto = new DiscountedCashFlowDTO("DUMMY", "2024-09-26", 15.5, 14);
    private final PriceTargetConsensusDTO ptcDto = new PriceTargetConsensusDTO("DUMMY", 20, 10, 16, 15);
    private final PriceTargetSummaryDTO ptsDto = new PriceTargetSummaryDTO("DUMMY", 2, 16, 5, 14);

    @Test
    void testWithAllData(){
        final RecordHolder recordHolder = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, this.ptcDto, this.ptsDto);
        final String expectedFormat = "{\"ticker\":\"DUMMY\",\"discountedCashFlow\":{\"date\":\"2024-09-26\",\"dcf\":15.5,\"stockPrice\":14},\"priceTargetConsensus\":{\"lastQuarterAvgPriceTarget\":14,\"lastMonthAvgPriceTarget\":16,\"lastQuarter\":5,\"lastMonth\":2}}";
        assertEquals(expectedFormat, this.sut.getFormattedResponseBody(recordHolder, null));
    }

    @Test
    void testOnlyError(){
        final RecordHolder recordHolder = RecordHolder.newRecordHolder("DUMMY", null, null, null);
        final String expectedFormat = "{\"ticker\":\"DUMMY\",\"error\":\"No API key found!\"}";
        assertEquals(expectedFormat, this.sut.getFormattedResponseBody(recordHolder, "No API key found!"));
    }
}
