package com.szilberhornz.valueinvdata.services.stockvaluation.persistence;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.RecordHolder;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.persistence.api.ValuationDBRepository;
import com.szilberhornz.valueinvdata.services.stockvaluation.persistence.inmem.InMemoryDBDataSourceFactory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Writing this test class using Mockito would take an immense amount of time, but luckily, we have an
 * in-memory DB at hand!
 */
class ValuationDBRepositoryImplTest {

    //only init once
    private static final DataSource IN_MEMORY_DATA_SOURCE = new InMemoryDBDataSourceFactory("inMemTestDbInitializer.txt").getValuationDbDataSource();

    private final ValuationDBRepositoryImpl sut = new ValuationDBRepositoryImpl(IN_MEMORY_DATA_SOURCE);


    @Test
    void queryForTickerShouldSucceed(){
        final RecordHolder recordHolder = this.sut.queryRecords("MSFT");
        final DiscountedCashFlowDTO dcfDto = recordHolder.getDiscountedCashFlowDto();
        final PriceTargetSummaryDTO ptsDto = recordHolder.getPriceTargetSummaryDto();
        final PriceTargetConsensusDTO ptcDto = recordHolder.getPriceTargetConsensusDto();
        assertEquals(455.76, dcfDto.dcf());
        assertEquals(433.51, dcfDto.stockPrice());
        assertEquals("2024-09-23", dcfDto.dateString());
        assertEquals(6, ptsDto.lastMonth());
        assertEquals(24, ptsDto.lastQuarter());
        assertEquals(480.3, ptsDto.lastMonthAvgPriceTarget());
        assertEquals(432.83, ptsDto.lastQuarterAvgPriceTarget());
        assertEquals(540, ptcDto.targetHigh());
        assertEquals(410, ptcDto.targetLow());
        assertEquals(454.83, ptcDto.targetConsensus());
        assertEquals(460, ptcDto.targetMedian());
    }

    @Test
    void invalidTickerForRecordShouldReturnNull(){
        final RecordHolder recordHolder = this.sut.queryRecords("INVALID_TICKER");
        assertNull(recordHolder);
    }

    @Test
    void incompleteRecordShouldAlsoBeReturned(){
        final RecordHolder recordHolder = this.sut.queryRecords("CSCO");
        final DiscountedCashFlowDTO dcfDto = recordHolder.getDiscountedCashFlowDto();
        final PriceTargetSummaryDTO ptsDto = recordHolder.getPriceTargetSummaryDto();
        final PriceTargetConsensusDTO ptcDto = recordHolder.getPriceTargetConsensusDto();
        assertEquals(72.31, dcfDto.dcf());
        assertEquals(52.19, dcfDto.stockPrice());
        assertEquals("2024-09-23", dcfDto.dateString());
        assertNull(ptcDto);
        assertNull(ptsDto);
    }

    @Test
    void queryDcfShouldSucceed(){
        final DiscountedCashFlowDTO dcfDto = this.sut.queryDiscountedCashFlowData("MSFT");
        assertEquals(455.76, dcfDto.dcf());
        assertEquals(433.51, dcfDto.stockPrice());
        assertEquals("2024-09-23", dcfDto.dateString());
    }


    @Test
    void noResultDcfShouldReturnNull(){
        final DiscountedCashFlowDTO dcfDto = this.sut.queryDiscountedCashFlowData("INVALID_TICKER");
        assertNull(dcfDto);
    }

    @Test
    void queryPtsShouldSucceed(){
        final PriceTargetSummaryDTO ptsDto = this.sut.queryPriceTargetSummaryData("MSFT");
        assertEquals(6, ptsDto.lastMonth());
        assertEquals(24, ptsDto.lastQuarter());
        assertEquals(480.3, ptsDto.lastMonthAvgPriceTarget());
        assertEquals(432.83, ptsDto.lastQuarterAvgPriceTarget());
    }

    @Test
    void noResultPtsShouldReturnNull(){
        final PriceTargetSummaryDTO ptsDto = this.sut.queryPriceTargetSummaryData("INVALID_TICKER");
        assertNull(ptsDto);
    }

    @Test
    void queryPtcShouldSucceed(){
        final PriceTargetConsensusDTO ptcDto = this.sut.queryPriceTargetConsensusData("MSFT");
        assertEquals(540, ptcDto.targetHigh());
        assertEquals(410, ptcDto.targetLow());
        assertEquals(454.83, ptcDto.targetConsensus());
        assertEquals(460, ptcDto.targetMedian());
    }

    @Test
    void noResultPtcShouldReturnNull(){
        final PriceTargetConsensusDTO ptcDto = this.sut.queryPriceTargetConsensusData("INVALID_TICKER");
        assertNull(ptcDto);
    }

    @Test
    void insertAndQueryDataShouldSucceed(){
        final DiscountedCashFlowDTO dcfDTO = new DiscountedCashFlowDTO("DUMMY", "2023-09-24", 66.66, 77.77);
        final PriceTargetSummaryDTO ptsDTO = new PriceTargetSummaryDTO("DUMMY", 5, 11.1, 5, 11.2);
        final PriceTargetConsensusDTO ptcDTO = new PriceTargetConsensusDTO("DUMMY", 15, 10, 15,12);
        final RecordHolder recordHolder = RecordHolder.newRecordHolder("DUMMY", dcfDTO, ptcDTO, ptsDTO);
        this.sut.insertFullRecord(recordHolder);
        final RecordHolder result = this.sut.queryRecords("DUMMY");
        assertEquals(dcfDTO, result.getDiscountedCashFlowDto());
        assertEquals(ptsDTO, result.getPriceTargetSummaryDto());
        assertEquals(ptcDTO, result.getPriceTargetConsensusDto());
    }

    @Test
    void nullDataShouldNotBeInserted(){
        final RecordHolder holder = RecordHolder.newRecordHolder("NONEXISTENT", null, null, null);
        this.sut.insertFullRecord(holder);
        final RecordHolder result = this.sut.queryRecords("DUMMY");
        assertNull(result);
    }

    @Test
    void sqlExceptionWhileQueryRecordsShouldReturnNull() throws SQLException {
        final DataSource mockDataSource = Mockito.mock(DataSource.class);
        final SQLException expectedException = new SQLException("testException");
        when(mockDataSource.getConnection()).thenThrow(expectedException);
        final ValuationDBRepository repo = new ValuationDBRepositoryImpl(mockDataSource);
        final RecordHolder recordHolder = repo.queryRecords("MSFT");
        assertNull(recordHolder);
    }

    @Test
    void sqlExceptionWhileQueryForDcfShouldReturnNull() throws SQLException {
        final DataSource mockDataSource = Mockito.mock(DataSource.class);
        final SQLException expectedException = new SQLException("testException");
        when(mockDataSource.getConnection()).thenThrow(expectedException);
        final ValuationDBRepository repo = new ValuationDBRepositoryImpl(mockDataSource);
        final DiscountedCashFlowDTO dto = repo.queryDiscountedCashFlowData("MSFT");
        assertNull(dto);
    }

    @Test
    void sqlExceptionWhileQueryForPtsShouldReturnNull() throws SQLException {
        final DataSource mockDataSource = Mockito.mock(DataSource.class);
        final SQLException expectedException = new SQLException("testException");
        when(mockDataSource.getConnection()).thenThrow(expectedException);
        final ValuationDBRepository repo = new ValuationDBRepositoryImpl(mockDataSource);
        final PriceTargetSummaryDTO dto = repo.queryPriceTargetSummaryData("MSFT");
        assertNull(dto);
    }

    @Test
    void sqlExceptionWhileQueryForPtcShouldReturnNull() throws SQLException {
        final DataSource mockDataSource = Mockito.mock(DataSource.class);
        final SQLException expectedException = new SQLException("testException");
        when(mockDataSource.getConnection()).thenThrow(expectedException);
        final ValuationDBRepository repo = new ValuationDBRepositoryImpl(mockDataSource);
        final PriceTargetConsensusDTO dto = repo.queryPriceTargetConsensusData("MSFT");
        assertNull(dto);
    }

    @Test
    void insertionFailureShouldRetryOnce() throws SQLException {
        final DataSource mockDataSource = Mockito.mock(DataSource.class);
        final SQLException retryableException = new SQLException("testException", "bad" , 1205, new SQLException());
        final SQLException nestedRetryableException = new SQLException(retryableException);
        final Connection connectionMock = Mockito.mock(Connection.class);
        final PreparedStatement stmtMock = Mockito.mock(PreparedStatement.class);
        //throw two retryables
        when(stmtMock.executeUpdate()).thenThrow(nestedRetryableException).thenThrow(retryableException);
        when(connectionMock.prepareStatement(any())).thenReturn(stmtMock).thenReturn(stmtMock);
        when(mockDataSource.getConnection()).thenReturn(connectionMock).thenReturn(connectionMock);
        //we only insert dcfDTo so the PreparedStatement mock should only be called 2 times if there is a retry
        final DiscountedCashFlowDTO dcfDTO = new DiscountedCashFlowDTO("DUMMY2", "2023-09-24", 66.66, 77.77);
        final RecordHolder recordHolder = RecordHolder.newRecordHolder("DUMMY2", dcfDTO, null, null);
        final ValuationDBRepository repo = new ValuationDBRepositoryImpl(mockDataSource);
        repo.insertFullRecord(recordHolder);
        verify(stmtMock, times(2)).executeUpdate();
    }

    @Test
    void insertionFailureShouldNotRetry() throws SQLException {
        final DataSource mockDataSource = Mockito.mock(DataSource.class);
        final SQLException nonRetryableException = new SQLException("testException");
        final Connection connectionMock = Mockito.mock(Connection.class);
        final PreparedStatement stmtMock = Mockito.mock(PreparedStatement.class);
        when(stmtMock.executeUpdate()).thenThrow(nonRetryableException).thenReturn(1);
        when(connectionMock.prepareStatement(any())).thenReturn(stmtMock).thenReturn(stmtMock);
        when(mockDataSource.getConnection()).thenReturn(connectionMock).thenReturn(connectionMock);
        //we only insert dcfDTo so the PreparedStatement mock should only be called 1 times if there is no retry
        final DiscountedCashFlowDTO dcfDTO = new DiscountedCashFlowDTO("DUMMY2", "2023-09-24", 66.66, 77.77);
        final RecordHolder recordHolder = RecordHolder.newRecordHolder("DUMMY2", dcfDTO, null, null);
        final ValuationDBRepository repo = new ValuationDBRepositoryImpl(mockDataSource);
        repo.insertFullRecord(recordHolder);
        verify(stmtMock, times(1)).executeUpdate();
    }
}
