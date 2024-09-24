package com.szilberhornz.valueinvdata.services.stockvaluation.core;

import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

class RecordMapperTest {

    @Test
    void successfulDcfConstructionFromHttpResponse(){
        final String validStringResponse = "[\n" +
                "\t{\n" +
                "\t\t\"symbol\": \"AAPL\",\n" +
                "\t\t\"date\": \"2023-03-03\",\n" +
                "\t\t\"dcf\": 151.0983806294802,\n" +
                "\t\t\"Stock Price\": 149.65\n" +
                "\t}\n" +
                "]";
        final HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.body()).thenReturn(validStringResponse);
        final DiscountedCashFlowDTO result = RecordMapper.newDcfDto(response);
        assertEquals("AAPL", result.ticker());
        assertEquals("2023-03-03", result.dateString());
        assertEquals(151.0983806294802, result.dcf());
        assertEquals(149.65, result.stockPrice());
    }

    @Test
    void failedDcfContructionShouldReturnNull(){
        final String invalidStringResponse = "[\n" +
                "\t{\n" +
                "\t\t\"symbol\": \"AAPL\",\n" +
                "\t\t\"dcf\": 151.0983806294802,\n" +
                "\t\t\"Stock Price\": 149.65\n" +
                "\t}\n" +
                "]";
        final HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.body()).thenReturn(invalidStringResponse);
        final DiscountedCashFlowDTO result = RecordMapper.newDcfDto(response);
        assertNull(result);
    }

    @Test
    void successfulPtcConstructionFromHttpResponse() {
        final String validStringResponse = "[\n" +
                "\t{\n" +
                "\t\t\"symbol\": \"AAPL\",\n" +
                "\t\t\"targetHigh\": 240,\n" +
                "\t\t\"targetLow\": 110,\n" +
                "\t\t\"targetConsensus\": 189.18,\n" +
                "\t\t\"targetMedian\": 195\n" +
                "\t}\n" +
                "]";
        final HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.body()).thenReturn(validStringResponse);
        final PriceTargetConsensusDTO result = RecordMapper.newPtcDto(response);
        assertEquals(240, result.targetHigh());
        assertEquals(110, result.targetLow());
        assertEquals(189.18, result.targetConsensus());
        assertEquals(195, result.targetMedian());
    }

    @Test
    void failedPtcContructionShouldReturnNull() {
        final String invalidStringResponse = "[\n" +
                "\t{\n" +
                "\t\t\"symbol\": \"AAPL\",\n" +
                "\t\t\"targetHigh\": 240,\n" +
                "\t\t\"targetConsensus\": 189.18,\n" +
                "\t\t\"targetMedian\": 195\n" +
                "\t}\n" +
                "]";
        final HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.body()).thenReturn(invalidStringResponse);
        final PriceTargetConsensusDTO result = RecordMapper.newPtcDto(response);
        assertNull(result);
    }

    @Test
    void successfulPtsConstructionFromHttpResponse() {
        final String validStringResponse = "[\n" +
                "\t{\n" +
                "\t\t\"symbol\": \"AAPL\",\n" +
                "\t\t\"lastMonth\": 5,\n" +
                "\t\t\"lastMonthAvgPriceTarget\": 220.2,\n" +
                "\t\t\"lastQuarter\": 11,\n" +
                "\t\t\"lastQuarterAvgPriceTarget\": 217.18,\n" +
                "\t\t\"lastYear\": 46,\n" +
                "\t\t\"lastYearAvgPriceTarget\": 186.8,\n" +
                "\t\t\"allTime\": 113,\n" +
                "\t\t\"allTimeAvgPriceTarget\": 186.31,\n" +
                "\t\t\"publishers\": \"[\\\"Benzinga\\\",\\\"TheFly\\\",\\\"Pulse 2.0\\\",\\\"MarketWatch\\\",\\\"TipRanks Contributor\\\",\\\"Investing\\\",\\\"StreetInsider\\\",\\\"Barrons\\\",\\\"Investor's Business Daily\\\"]\"\n" +
                "\t}\n" +
                "]";
        final HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.body()).thenReturn(validStringResponse);
        final PriceTargetSummaryDTO result = RecordMapper.newPtsDto(response);
        assertEquals(5, result.lastMonth());
        assertEquals(220.2, result.lastMonthAvgPriceTarget());
        assertEquals(11, result.lastQuarter());
        assertEquals(217.18, result.lastQuarterAvgPriceTarget());
    }

    @Test
    void failedPtsContructionShouldReturnNull() {
        final String invalidStringResponse = "[\n" +
                "\t{\n" +
                "\t\t\"symbol\": \"AAPL\",\n" +
                "\t\t\"lastMonth\": 5,\n" +
                "\t\t\"lastQuarter\": 11,\n" +
                "\t\t\"lastQuarterAvgPriceTarget\": 217.18,\n" +
                "\t\t\"lastYear\": 46,\n" +
                "\t\t\"lastYearAvgPriceTarget\": 186.8,\n" +
                "\t\t\"allTime\": 113,\n" +
                "\t\t\"allTimeAvgPriceTarget\": 186.31,\n" +
                "\t\t\"publishers\": \"[\\\"Benzinga\\\",\\\"TheFly\\\",\\\"Pulse 2.0\\\",\\\"MarketWatch\\\",\\\"TipRanks Contributor\\\",\\\"Investing\\\",\\\"StreetInsider\\\",\\\"Barrons\\\",\\\"Investor's Business Daily\\\"]\"\n" +
                "\t}\n" +
                "]";
        final HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.body()).thenReturn(invalidStringResponse);
        final PriceTargetSummaryDTO result = RecordMapper.newPtsDto(response);
        assertNull(result);
    }
}
