package com.szilberhornz.valueinvdata.services.stockvaluation.utility;

import com.szilberhornz.valueinvdata.services.stockvaluation.model.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.model.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.model.record.PriceTargetSummaryDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.http.HttpResponse;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class RecordMapperTest {

    @Test
    void successfulDcfConstructionFromHttpResponse(){
        final String validStringResponse = """
                [
                \t{
                \t\t"symbol": "AAPL",
                \t\t"date": "2023-03-03",
                \t\t"dcf": 151.0983806294802,
                \t\t"Stock Price": 149.65
                \t}
                ]""";
        final HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.body()).thenReturn(validStringResponse);
        final DiscountedCashFlowDTO result = RecordMapper.newDcfDto(response);
        assertEquals("AAPL", result.ticker());
        assertEquals("2023-03-03", result.dateString());
        assertEquals(151.0983806294802, result.dcf());
        assertEquals(149.65, result.stockPrice());
    }

    @Test
    void failedDcfConstructionShouldReturnNull(){
        final String invalidStringResponse = """
                [
                \t{
                \t\t"symbol": "AAPL",
                \t\t"dcf": 151.0983806294802,
                \t\t"Stock Price": 149.65
                \t}
                ]""";
        final HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.body()).thenReturn(invalidStringResponse);
        final DiscountedCashFlowDTO result = RecordMapper.newDcfDto(response);
        assertNull(result);
    }

    @Test
    void successfulPtcConstructionFromHttpResponse() {
        final String validStringResponse = """
                [
                \t{
                \t\t"symbol": "AAPL",
                \t\t"targetHigh": 240,
                \t\t"targetLow": 110,
                \t\t"targetConsensus": 189.18,
                \t\t"targetMedian": 195
                \t}
                ]""";
        final HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.body()).thenReturn(validStringResponse);
        final PriceTargetConsensusDTO result = RecordMapper.newPtcDto(response);
        assertEquals(240, result.targetHigh());
        assertEquals(110, result.targetLow());
        assertEquals(189.18, result.targetConsensus());
        assertEquals(195, result.targetMedian());
    }

    @Test
    void failedPtcConstructionShouldReturnNull() {
        final String invalidStringResponse = """
                [
                \t{
                \t\t"symbol": "AAPL",
                \t\t"targetHigh": 240,
                \t\t"targetConsensus": 189.18,
                \t\t"targetMedian": 195
                \t}
                ]""";
        final HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.body()).thenReturn(invalidStringResponse);
        final PriceTargetConsensusDTO result = RecordMapper.newPtcDto(response);
        assertNull(result);
    }

    @Test
    void successfulPtsConstructionFromHttpResponse() {
        final String validStringResponse = """
                [
                \t{
                \t\t"symbol": "AAPL",
                \t\t"lastMonth": 5,
                \t\t"lastMonthAvgPriceTarget": 220.2,
                \t\t"lastQuarter": 11,
                \t\t"lastQuarterAvgPriceTarget": 217.18,
                \t\t"lastYear": 46,
                \t\t"lastYearAvgPriceTarget": 186.8,
                \t\t"allTime": 113,
                \t\t"allTimeAvgPriceTarget": 186.31,
                \t\t"publishers": "[\\"Benzinga\\",\\"TheFly\\",\\"Pulse 2.0\\",\\"MarketWatch\\",\\"TipRanks Contributor\\",\\"Investing\\",\\"StreetInsider\\",\\"Barrons\\",\\"Investor's Business Daily\\"]"
                \t}
                ]""";
        final HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.body()).thenReturn(validStringResponse);
        final PriceTargetSummaryDTO result = RecordMapper.newPtsDto(response);
        assertEquals(5, result.lastMonth());
        assertEquals(220.2, result.lastMonthAvgPriceTarget());
        assertEquals(11, result.lastQuarter());
        assertEquals(217.18, result.lastQuarterAvgPriceTarget());
    }

    @Test
    void failedPtsConstructionShouldReturnNull() {
        final String invalidStringResponse = """
                [
                \t{
                \t\t"symbol": "AAPL",
                \t\t"lastMonth": 5,
                \t\t"lastQuarter": 11,
                \t\t"lastQuarterAvgPriceTarget": 217.18,
                \t\t"lastYear": 46,
                \t\t"lastYearAvgPriceTarget": 186.8,
                \t\t"allTime": 113,
                \t\t"allTimeAvgPriceTarget": 186.31,
                \t\t"publishers": "[\\"Benzinga\\",\\"TheFly\\",\\"Pulse 2.0\\",\\"MarketWatch\\",\\"TipRanks Contributor\\",\\"Investing\\",\\"StreetInsider\\",\\"Barrons\\",\\"Investor's Business Daily\\"]"
                \t}
                ]""";
        final HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.body()).thenReturn(invalidStringResponse);
        final PriceTargetSummaryDTO result = RecordMapper.newPtsDto(response);
        assertNull(result);
    }

    @Test
    void illegalResultSetDataShouldThrowException() throws SQLException {
        final ResultSet resultSetMock = Mockito.mock(ResultSet.class);
        final ResultSetMetaData rsmMock = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(rsmMock.getColumnCount()).thenReturn(1);
        Mockito.when(resultSetMock.getMetaData()).thenReturn(rsmMock);
        Mockito.when(resultSetMock.next())
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);
        Mockito.when(resultSetMock.getObject(1))
                .thenReturn("test")
                .thenReturn("test")
                .thenReturn("test")
                .thenReturn("test")
                .thenReturn("test");
        final Exception exception = assertThrows(IllegalStateException.class, () -> RecordMapper.newDcfDto(resultSetMock));
        assertEquals("The ResultSet unexpectedly held more than one row of data! This should not have happened as " +
                "the ticker is the primary key in the db tables and we only have 4 columns!", exception.getMessage());
    }
}
