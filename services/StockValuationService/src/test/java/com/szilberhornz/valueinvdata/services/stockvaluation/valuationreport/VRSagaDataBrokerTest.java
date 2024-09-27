package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.RecordHolder;
import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationServerCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.FMPResponseHandler;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr.ApiKeyException;
import com.szilberhornz.valueinvdata.services.stockvaluation.persistence.api.ValuationDBRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;

class VRSagaDataBrokerTest {

    private final ValuationDBRepository dbRepositoryMock = Mockito.mock(ValuationDBRepository.class);
    private final ValuationServerCache serverCacheMock = Mockito.mock(ValuationServerCache.class);
    private final FMPResponseHandler fmpHandlerMock = Mockito.mock(FMPResponseHandler.class);


    private final DiscountedCashFlowDTO dcfDto = new DiscountedCashFlowDTO("DUMMY", "2024-09-26", 15.5, 14);
    private final PriceTargetConsensusDTO ptcDto = new PriceTargetConsensusDTO("DUMMY", 20, 10, 16, 15);
    private final PriceTargetSummaryDTO ptsDto = new PriceTargetSummaryDTO("DUMMY", 2, 16, 5, 14);

    @Test
    void getFromCacheTest(){
        final VRSagaDataBroker sut = new VRSagaDataBroker(this.dbRepositoryMock, this.serverCacheMock, this.fmpHandlerMock);
        sut.getFromCache("DUMMY");
        Mockito.verify(this.serverCacheMock, Mockito.times(1)).get("DUMMY");
    }

    @Test
    void getDataFromDbShouldRunFullQueryIfCachedRecordIsNull(){
        final VRSagaDataBroker sut = new VRSagaDataBroker(this.dbRepositoryMock, this.serverCacheMock, this.fmpHandlerMock);
        sut.getDataFromDb(null, "DUMMY");
        Mockito.verify(this.dbRepositoryMock, Mockito.times(1)).queryRecords("DUMMY");
    }

    @Test
    void getDataFromDbShouldRunQueryForEachMissingDTO(){
        final VRSagaDataBroker sut = new VRSagaDataBroker(this.dbRepositoryMock, this.serverCacheMock, this.fmpHandlerMock);
        final RecordHolder recordHolder = RecordHolder.newRecordHolder("DUMMY", null, null, null);
        sut.getDataFromDb(recordHolder, "DUMMY");
        Mockito.verify(this.dbRepositoryMock, Mockito.times(1)).queryPriceTargetConsensusData("DUMMY");
        Mockito.verify(this.dbRepositoryMock, Mockito.times(1)).queryPriceTargetSummaryData("DUMMY");
        Mockito.verify(this.dbRepositoryMock, Mockito.times(1)).queryDiscountedCashFlowData("DUMMY");
    }

    @Test
    void getDataFromFmpApiShouldGetAllMissingDTO() throws Throwable {
        final VRSagaDataBroker sut = new VRSagaDataBroker(this.dbRepositoryMock, this.serverCacheMock, this.fmpHandlerMock);
        sut.getDataFromFmpApi(null, "DUMMY", 2500);
        Mockito.verify(this.fmpHandlerMock, Mockito.times(1)).getDiscountedCashFlowReportFromFmpApi("DUMMY");
        Mockito.verify(this.fmpHandlerMock, Mockito.times(1)).getPriceTargetConsensusReportFromFmpApi("DUMMY");
        Mockito.verify(this.fmpHandlerMock, Mockito.times(1)).getPriceTargetSummaryReportFromFmpApi("DUMMY");
    }

    @Test
    void getDataFromFmpApiFullResultTest() throws Throwable {
        Mockito.when(this.fmpHandlerMock.getDiscountedCashFlowReportFromFmpApi("DUMMY")).thenReturn(this.dcfDto);
        Mockito.when(this.fmpHandlerMock.getPriceTargetConsensusReportFromFmpApi("DUMMY")).thenReturn(this.ptcDto);
        Mockito.when(this.fmpHandlerMock.getPriceTargetSummaryReportFromFmpApi("DUMMY")).thenReturn(this.ptsDto);
        final VRSagaDataBroker sut = new VRSagaDataBroker(this.dbRepositoryMock, this.serverCacheMock, this.fmpHandlerMock);
        final RecordHolder result = sut.getDataFromFmpApi(null, "DUMMY", 2500);
        assertEquals(this.dcfDto, result.getDiscountedCashFlowDto());
        assertEquals(this.ptcDto, result.getPriceTargetConsensusDto());
        assertEquals(this.ptsDto, result.getPriceTargetSummaryDto());
        assertEquals("DUMMY", result.getTicker());
    }

    @Test
    void getDataFromFmpApiShouldGetMissingDcf() throws Throwable {
        final VRSagaDataBroker sut = new VRSagaDataBroker(this.dbRepositoryMock, this.serverCacheMock, this.fmpHandlerMock);
        final RecordHolder recordFromDb = RecordHolder.newRecordHolder("DUMMY", null, this.ptcDto, this.ptsDto);
        sut.getDataFromFmpApi(recordFromDb, "DUMMY", 2500);
        Mockito.verify(this.fmpHandlerMock, Mockito.times(1)).getDiscountedCashFlowReportFromFmpApi("DUMMY");
        Mockito.verify(this.fmpHandlerMock, Mockito.times(0)).getPriceTargetConsensusReportFromFmpApi("DUMMY");
        Mockito.verify(this.fmpHandlerMock, Mockito.times(0)).getPriceTargetSummaryReportFromFmpApi("DUMMY");
    }

    @Test
    void getDataFromFmpApiShouldGetMissingPtc() throws Throwable {
        final VRSagaDataBroker sut = new VRSagaDataBroker(this.dbRepositoryMock, this.serverCacheMock, this.fmpHandlerMock);
        final RecordHolder recordFromDb = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, null, this.ptsDto);
        sut.getDataFromFmpApi(recordFromDb, "DUMMY", 2500);
        Mockito.verify(this.fmpHandlerMock, Mockito.times(0)).getDiscountedCashFlowReportFromFmpApi("DUMMY");
        Mockito.verify(this.fmpHandlerMock, Mockito.times(1)).getPriceTargetConsensusReportFromFmpApi("DUMMY");
        Mockito.verify(this.fmpHandlerMock, Mockito.times(0)).getPriceTargetSummaryReportFromFmpApi("DUMMY");
    }

    @Test
    void getDataFromFmpApiShouldGetMissingPts() throws Throwable {
        final VRSagaDataBroker sut = new VRSagaDataBroker(this.dbRepositoryMock, this.serverCacheMock, this.fmpHandlerMock);
        final RecordHolder recordFromDb = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, this.ptcDto, null);
        sut.getDataFromFmpApi(recordFromDb, "DUMMY", 2500);
        Mockito.verify(this.fmpHandlerMock, Mockito.times(0)).getDiscountedCashFlowReportFromFmpApi("DUMMY");
        Mockito.verify(this.fmpHandlerMock, Mockito.times(0)).getPriceTargetConsensusReportFromFmpApi("DUMMY");
        Mockito.verify(this.fmpHandlerMock, Mockito.times(1)).getPriceTargetSummaryReportFromFmpApi("DUMMY");
    }

    @Test
    void getDataFromApiShouldReturnExecutionExceptionCause() {
        final ApiKeyException apiKeyException = new ApiKeyException("test");
        Mockito.when(this.fmpHandlerMock.getDiscountedCashFlowReportFromFmpApi("DUMMY")).thenThrow(apiKeyException);
        final VRSagaDataBroker sut = new VRSagaDataBroker(this.dbRepositoryMock, this.serverCacheMock, this.fmpHandlerMock);
        final RecordHolder result = sut.getDataFromFmpApi(null, "DUMMY", 2500);
        assertInstanceOf(ApiKeyException.class, result.getCauseOfNullDtos());
        assertEquals("test", result.getCauseOfNullDtos().getMessage());
    }

    @Test
    void persistDataShouldInsertToDbAndCacheWhenMissing(){
        final RecordHolder recordFromApi = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, this.ptcDto, this.ptsDto);
        final VRSagaDataBroker sut = new VRSagaDataBroker(this.dbRepositoryMock, this.serverCacheMock, this.fmpHandlerMock);
        sut.persistData("DUMMY", null, null, recordFromApi);
        Mockito.verify(this.serverCacheMock, times(1)).put("DUMMY", this.dcfDto);
        Mockito.verify(this.serverCacheMock, times(1)).put("DUMMY", this.ptcDto);
        Mockito.verify(this.serverCacheMock, times(1)).put("DUMMY", this.ptsDto);
        Mockito.verify(this.dbRepositoryMock, times(1)).insertFullRecord(recordFromApi);
    }

    @Test
    void persistDataShouldInsertToDbAndCacheWhenPartialMiss(){
        final RecordHolder recordFromApi = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, this.ptcDto, this.ptsDto);
        final RecordHolder recordFromCache = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, null, null);
        final RecordHolder recordFromDb = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, null, null);
        final VRSagaDataBroker sut = new VRSagaDataBroker(this.dbRepositoryMock, this.serverCacheMock, this.fmpHandlerMock);
        sut.persistData("DUMMY", recordFromCache, recordFromDb, recordFromApi);
        Mockito.verify(this.serverCacheMock, times(0)).put("DUMMY", this.dcfDto);
        Mockito.verify(this.serverCacheMock, times(1)).put("DUMMY", this.ptcDto);
        Mockito.verify(this.serverCacheMock, times(1)).put("DUMMY", this.ptsDto);
        Mockito.verify(this.dbRepositoryMock, times(1)).insertPriceTargetSummaryData(recordFromApi.getPriceTargetSummaryDto());
        Mockito.verify(this.dbRepositoryMock, times(1)).insertPriceTargetConsensusData(recordFromApi.getPriceTargetConsensusDto());
    }

    @Test
    void persistDataShouldInsertToCacheWhenPartialMiss(){
        final RecordHolder recordFromApi = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, this.ptcDto, this.ptsDto);
        final RecordHolder recordFromCache = RecordHolder.newRecordHolder("DUMMY", null, this.ptcDto, this.ptsDto);
        final RecordHolder recordFromDb = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, this.ptcDto, this.ptsDto);
        final VRSagaDataBroker sut = new VRSagaDataBroker(this.dbRepositoryMock, this.serverCacheMock, this.fmpHandlerMock);
        sut.persistData("DUMMY", recordFromCache, recordFromDb, recordFromApi);
        Mockito.verify(this.serverCacheMock, times(1)).put("DUMMY", this.dcfDto);
        Mockito.verify(this.serverCacheMock, times(0)).put("DUMMY", this.ptcDto);
        Mockito.verify(this.serverCacheMock, times(0)).put("DUMMY", this.ptsDto);
        Mockito.verify(this.dbRepositoryMock, times(0)).insertFullRecord(recordFromApi);
    }
}
