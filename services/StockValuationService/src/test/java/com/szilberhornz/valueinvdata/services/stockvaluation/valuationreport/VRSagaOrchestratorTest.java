package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport;

import com.szilberhornz.valueinvdata.services.stockvaluation.utility.cache.RecordHolder;
import com.szilberhornz.valueinvdata.services.stockvaluation.utility.cache.TickerCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.model.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.model.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.model.record.PriceTargetSummaryDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.fmp.RateLimitReachedException;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.fmp.authr.ApiKeyException;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.circuitbreaker.VRSagaDefaultCircuitBreaker;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.formatter.ValuationResponseBodyJSONFormatter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

class VRSagaOrchestratorTest {

    private final TickerCache tickerCacheMock = Mockito.mock(TickerCache.class);
    private final VRSagaDataBroker dataBrokerMock = Mockito.mock(VRSagaDataBroker.class);

    private final DiscountedCashFlowDTO dcfDto = new DiscountedCashFlowDTO("DUMMY", "2024-09-26", 15.5, 14);
    private final PriceTargetConsensusDTO ptcDto = new PriceTargetConsensusDTO("DUMMY", 20, 10, 16, 15);
    private final PriceTargetSummaryDTO ptsDto = new PriceTargetSummaryDTO("DUMMY", 2, 16, 5, 14);

    @Test
    void http403ForInvalidTicker(){
        Mockito.when(this.tickerCacheMock.tickerExists("DUMMY")).thenReturn(false);
        final VRSagaOrchestrator sut = new VRSagaOrchestrator(this.tickerCacheMock, new ValuationResponseBodyJSONFormatter(), new VRSagaDefaultCircuitBreaker(), this.dataBrokerMock);
        final ValuationReport result = sut.getValuationResponse("DUMMY");
        assertEquals(403, result.getStatusCode());
        assertEquals("The server only responds to valuation report requests " +
                "for real tickers! The ticker DUMMY is not a valid ticker! Please try again with a valid ticker!", result.getErrorMessage());
    }

    @Test
    void cachedItemShouldBeReturnedImmediately() {
        Mockito.when(this.tickerCacheMock.tickerExists("DUMMY")).thenReturn(true);
        final RecordHolder cachedRecord = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, this.ptcDto, this.ptsDto);
        Mockito.when(this.dataBrokerMock.getFromCache("DUMMY")).thenReturn(cachedRecord);
        final VRSagaOrchestrator sut = new VRSagaOrchestrator(this.tickerCacheMock, new ValuationResponseBodyJSONFormatter(), new VRSagaDefaultCircuitBreaker(), this.dataBrokerMock);
        final ValuationReport result = sut.getValuationResponse("DUMMY");
        Mockito.verify(this.dataBrokerMock, Mockito.times(0)).getDataFromDb( any(), any());
        Mockito.verify(this.dataBrokerMock, Mockito.times(0)).getDataFromFmpApi( any(), any(), anyLong());
        final String expectedBody = "{\"ticker\":\"DUMMY\",\"discountedCashFlow\":{\"date\":\"2024-09-26\",\"dcf\":15.5,\"stockPrice\":14},\"priceTargetConsensus\":{\"lastQuarterAvgPriceTarget\":14,\"lastMonthAvgPriceTarget\":16,\"lastQuarter\":5,\"lastMonth\":2}}";
        assertEquals(200, result.getStatusCode());
        assertEquals(expectedBody, result.getMessageBody());
    }

    @Test
    void nullCachedItemShouldTriggerDbCall() {
        Mockito.when(this.tickerCacheMock.tickerExists("DUMMY")).thenReturn(true);
        Mockito.when(this.dataBrokerMock.getFromCache("DUMMY")).thenReturn(null);
        final RecordHolder dbRecord = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, this.ptcDto, this.ptsDto);
        Mockito.when(this.dataBrokerMock.getDataFromDb(null,"DUMMY")).thenReturn(dbRecord);
        final VRSagaOrchestrator sut = new VRSagaOrchestrator(this.tickerCacheMock, new ValuationResponseBodyJSONFormatter(), new VRSagaDefaultCircuitBreaker(), this.dataBrokerMock);
        final ValuationReport result = sut.getValuationResponse("DUMMY");
        Mockito.verify(this.dataBrokerMock, Mockito.times(1)).getDataFromDb( any(), any());
        Mockito.verify(this.dataBrokerMock, Mockito.times(0)).getDataFromFmpApi( any(), any(), anyLong());
        final String expectedBody = "{\"ticker\":\"DUMMY\",\"discountedCashFlow\":{\"date\":\"2024-09-26\",\"dcf\":15.5,\"stockPrice\":14},\"priceTargetConsensus\":{\"lastQuarterAvgPriceTarget\":14,\"lastMonthAvgPriceTarget\":16,\"lastQuarter\":5,\"lastMonth\":2}}";
        assertEquals(200, result.getStatusCode());
        assertEquals(expectedBody, result.getMessageBody());
    }

    @Test
    void incompleteCachedItemShouldTriggerDbCall() {
        Mockito.when(this.tickerCacheMock.tickerExists("DUMMY")).thenReturn(true);
        final RecordHolder cachedRecord = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, this.ptcDto, null);
        Mockito.when(this.dataBrokerMock.getFromCache("DUMMY")).thenReturn(cachedRecord);
        final RecordHolder dbRecord = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, this.ptcDto, this.ptsDto);
        Mockito.when(this.dataBrokerMock.getDataFromDb(cachedRecord,"DUMMY")).thenReturn(dbRecord);
        final VRSagaOrchestrator sut = new VRSagaOrchestrator(this.tickerCacheMock, new ValuationResponseBodyJSONFormatter(), new VRSagaDefaultCircuitBreaker(), this.dataBrokerMock);
        final ValuationReport result = sut.getValuationResponse("DUMMY");
        Mockito.verify(this.dataBrokerMock, Mockito.times(1)).getDataFromDb( any(), any());
        Mockito.verify(this.dataBrokerMock, Mockito.times(0)).getDataFromFmpApi( any(), any(), anyLong());
        final String expectedBody = "{\"ticker\":\"DUMMY\",\"discountedCashFlow\":{\"date\":\"2024-09-26\",\"dcf\":15.5,\"stockPrice\":14},\"priceTargetConsensus\":{\"lastQuarterAvgPriceTarget\":14,\"lastMonthAvgPriceTarget\":16,\"lastQuarter\":5,\"lastMonth\":2}}";
        assertEquals(200, result.getStatusCode());
        assertEquals(expectedBody, result.getMessageBody());
    }

    @Test
    void illegalStateDbShouldReturnHttp500() {
        Mockito.when(this.tickerCacheMock.tickerExists("DUMMY")).thenReturn(true);
        final RecordHolder cachedRecord = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, this.ptcDto, null);
        Mockito.when(this.dataBrokerMock.getFromCache("DUMMY")).thenReturn(cachedRecord);
        Mockito.when(this.dataBrokerMock.getDataFromDb(cachedRecord,"DUMMY")).thenThrow(new IllegalStateException("Oops!"));
        final VRSagaOrchestrator sut = new VRSagaOrchestrator(this.tickerCacheMock, new ValuationResponseBodyJSONFormatter(), new VRSagaDefaultCircuitBreaker(), this.dataBrokerMock);
        final ValuationReport result = sut.getValuationResponse("DUMMY");
        Mockito.verify(this.dataBrokerMock, Mockito.times(1)).getDataFromDb( any(), any());
        Mockito.verify(this.dataBrokerMock, Mockito.times(0)).getDataFromFmpApi( any(), any(), anyLong());
        final String expectedErrorMsg = "{\"error\":\"The server encountered an unexpected internal error when trying to generate report for ticker DUMMY!\"}";
        assertEquals(500, result.getStatusCode());
        assertEquals(expectedErrorMsg, result.getMessageBody());
    }

    @Test
    void fmpApiShouldBeCalledIfNoCachedOrDbDataIsComplete() {
        Mockito.when(this.tickerCacheMock.tickerExists("DUMMY")).thenReturn(true);
        final RecordHolder cachedRecord = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, this.ptcDto, null);
        Mockito.when(this.dataBrokerMock.getFromCache("DUMMY")).thenReturn(cachedRecord);
        final RecordHolder dbRecord = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, this.ptcDto, null);
        Mockito.when(this.dataBrokerMock.getDataFromDb(cachedRecord,"DUMMY")).thenReturn(dbRecord);
        final RecordHolder fmpApiRecord = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, this.ptcDto, this.ptsDto);
        Mockito.when(this.dataBrokerMock.getDataFromFmpApi(dbRecord, "DUMMY", 2500L)).thenReturn(fmpApiRecord);
        final VRSagaOrchestrator sut = new VRSagaOrchestrator(this.tickerCacheMock, new ValuationResponseBodyJSONFormatter(), new VRSagaDefaultCircuitBreaker(), this.dataBrokerMock);
        final ValuationReport result = sut.getValuationResponse("DUMMY");
        final String expectedBody = "{\"ticker\":\"DUMMY\",\"discountedCashFlow\":{\"date\":\"2024-09-26\",\"dcf\":15.5,\"stockPrice\":14},\"priceTargetConsensus\":{\"lastQuarterAvgPriceTarget\":14,\"lastMonthAvgPriceTarget\":16,\"lastQuarter\":5,\"lastMonth\":2}}";
        assertEquals(200, result.getStatusCode());
        assertEquals(expectedBody, result.getMessageBody());
    }

    @Test
    void fmpApiShouldBeCalledIfEveryRecordIsNull() {
        Mockito.when(this.tickerCacheMock.tickerExists("DUMMY")).thenReturn(true);
        Mockito.when(this.dataBrokerMock.getFromCache("DUMMY")).thenReturn(null);
        Mockito.when(this.dataBrokerMock.getDataFromDb(null,"DUMMY")).thenReturn(null);
        final RecordHolder fmpApiRecord = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, this.ptcDto, this.ptsDto);
        Mockito.when(this.dataBrokerMock.getDataFromFmpApi(null, "DUMMY", 2500L)).thenReturn(fmpApiRecord);
        final VRSagaOrchestrator sut = new VRSagaOrchestrator(this.tickerCacheMock, new ValuationResponseBodyJSONFormatter(), new VRSagaDefaultCircuitBreaker(), this.dataBrokerMock);
        final ValuationReport result = sut.getValuationResponse("DUMMY");
        final String expectedBody = "{\"ticker\":\"DUMMY\",\"discountedCashFlow\":{\"date\":\"2024-09-26\",\"dcf\":15.5,\"stockPrice\":14},\"priceTargetConsensus\":{\"lastQuarterAvgPriceTarget\":14,\"lastMonthAvgPriceTarget\":16,\"lastQuarter\":5,\"lastMonth\":2}}";
        assertEquals(200, result.getStatusCode());
        assertEquals(expectedBody, result.getMessageBody());
    }

    @Test
    void fmpApiKeyExceptionShouldProduce401() {
        Mockito.when(this.tickerCacheMock.tickerExists("DUMMY")).thenReturn(true);
        Mockito.when(this.dataBrokerMock.getFromCache("DUMMY")).thenReturn(null);
        Mockito.when(this.dataBrokerMock.getDataFromDb(null,"DUMMY")).thenReturn(null);
        final ApiKeyException apiKeyException = new ApiKeyException("test!");
        final RecordHolder fmpApiRecord = RecordHolder.newRecordHolder("DUMMY", null, null, null, apiKeyException);
        Mockito.when(this.dataBrokerMock.getDataFromFmpApi(null, "DUMMY", 2500L)).thenReturn(fmpApiRecord);
        final VRSagaOrchestrator sut = new VRSagaOrchestrator(this.tickerCacheMock, new ValuationResponseBodyJSONFormatter(), new VRSagaDefaultCircuitBreaker(), this.dataBrokerMock);
        final ValuationReport result = sut.getValuationResponse("DUMMY");
        final String expectedBody = "{\"error\":\"test!\"}";
        assertEquals(401, result.getStatusCode());
        assertEquals(expectedBody, result.getMessageBody());
    }

    @Test
    void fmpRateLimitExceptionShouldProduce429() {
        Mockito.when(this.tickerCacheMock.tickerExists("DUMMY")).thenReturn(true);
        Mockito.when(this.dataBrokerMock.getFromCache("DUMMY")).thenReturn(null);
        Mockito.when(this.dataBrokerMock.getDataFromDb(null,"DUMMY")).thenReturn(null);
        final RateLimitReachedException limitReachedException = new RateLimitReachedException("test!");
        final RecordHolder fmpApiRecord = RecordHolder.newRecordHolder("DUMMY", null, null, null, limitReachedException);
        Mockito.when(this.dataBrokerMock.getDataFromFmpApi(null, "DUMMY", 2500L)).thenReturn(fmpApiRecord);
        final VRSagaOrchestrator sut = new VRSagaOrchestrator(this.tickerCacheMock, new ValuationResponseBodyJSONFormatter(), new VRSagaDefaultCircuitBreaker(), this.dataBrokerMock);
        final ValuationReport result = sut.getValuationResponse("DUMMY");
        final String expectedBody = "{\"error\":\"test!\"}";
        assertEquals(429, result.getStatusCode());
        assertEquals(expectedBody, result.getMessageBody());
    }

    @Test
    void fmpRandomExceptionShouldProduce500() {
        Mockito.when(this.tickerCacheMock.tickerExists("DUMMY")).thenReturn(true);
        Mockito.when(this.dataBrokerMock.getFromCache("DUMMY")).thenReturn(null);
        Mockito.when(this.dataBrokerMock.getDataFromDb(null,"DUMMY")).thenReturn(null);
        final RuntimeException runtimeException = new RuntimeException("unexpected!");
        Mockito.when(this.dataBrokerMock.getDataFromFmpApi(null, "DUMMY", 2500L)).thenThrow(runtimeException);
        final VRSagaOrchestrator sut = new VRSagaOrchestrator(this.tickerCacheMock, new ValuationResponseBodyJSONFormatter(), new VRSagaDefaultCircuitBreaker(), this.dataBrokerMock);
        final ValuationReport result = sut.getValuationResponse("DUMMY");
        final String expectedBody = "{\"error\":\"The server encountered an unexpected internal error when trying to generate report for ticker DUMMY!\"}";
        assertEquals(500, result.getStatusCode());
        assertEquals(expectedBody, result.getMessageBody());
    }

    @Test
    void fmpApiKeyExceptionWithPartialDataShouldProduce200() {
        Mockito.when(this.tickerCacheMock.tickerExists("DUMMY")).thenReturn(true);
        Mockito.when(this.dataBrokerMock.getFromCache("DUMMY")).thenReturn(null);
        final RecordHolder dbRecord = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, null, null);
        Mockito.when(this.dataBrokerMock.getDataFromDb(null,"DUMMY")).thenReturn(dbRecord);
        final ApiKeyException apiKeyException = new ApiKeyException("test!");
        final RecordHolder fmpApiRecord = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, null, null, apiKeyException);
        Mockito.when(this.dataBrokerMock.getDataFromFmpApi(dbRecord, "DUMMY", 2500L)).thenReturn(fmpApiRecord);
        final VRSagaOrchestrator sut = new VRSagaOrchestrator(this.tickerCacheMock, new ValuationResponseBodyJSONFormatter(), new VRSagaDefaultCircuitBreaker(), this.dataBrokerMock);
        final ValuationReport result = sut.getValuationResponse("DUMMY");
        final String expectedBody = "{\"ticker\":\"DUMMY\",\"discountedCashFlow\":{\"date\":\"2024-09-26\",\"dcf\":15.5,\"stockPrice\":14},\"error\":\"test!\"}";
        assertEquals(200, result.getStatusCode());
        assertEquals(expectedBody, result.getMessageBody());
        assertEquals("test!", result.getErrorMessage());
    }

    @Test
    void fmpRateLimitExceptionWithPartialDataShouldProduce200() {
        Mockito.when(this.tickerCacheMock.tickerExists("DUMMY")).thenReturn(true);
        Mockito.when(this.dataBrokerMock.getFromCache("DUMMY")).thenReturn(null);
        final RecordHolder dbRecord = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, null, null);
        Mockito.when(this.dataBrokerMock.getDataFromDb(null,"DUMMY")).thenReturn(dbRecord);
        final RateLimitReachedException limitReachedException = new RateLimitReachedException("test!");
        final RecordHolder fmpApiRecord = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, null, null, limitReachedException);
        Mockito.when(this.dataBrokerMock.getDataFromFmpApi(dbRecord, "DUMMY", 2500L)).thenReturn(fmpApiRecord);
        final VRSagaOrchestrator sut = new VRSagaOrchestrator(this.tickerCacheMock, new ValuationResponseBodyJSONFormatter(), new VRSagaDefaultCircuitBreaker(), this.dataBrokerMock);
        final ValuationReport result = sut.getValuationResponse("DUMMY");
        final String expectedBody = "{\"ticker\":\"DUMMY\",\"discountedCashFlow\":{\"date\":\"2024-09-26\",\"dcf\":15.5,\"stockPrice\":14},\"error\":\"test!\"}";
        assertEquals(200, result.getStatusCode());
        assertEquals(expectedBody, result.getMessageBody());
        assertEquals("test!", result.getErrorMessage());
    }

    @Test
    void fmpRandomExceptionButPartialDataShouldProduce200() {
        Mockito.when(this.tickerCacheMock.tickerExists("DUMMY")).thenReturn(true);
        Mockito.when(this.dataBrokerMock.getFromCache("DUMMY")).thenReturn(null);
        final RecordHolder dbRecord = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, null, null);
        Mockito.when(this.dataBrokerMock.getDataFromDb(null,"DUMMY")).thenReturn(dbRecord);
        final RuntimeException runtimeException = new RuntimeException("unexpected!");
        final RecordHolder fmpApiRecord = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, null, null, runtimeException);
        Mockito.when(this.dataBrokerMock.getDataFromFmpApi(dbRecord, "DUMMY", 2500L)).thenReturn(fmpApiRecord);
        final VRSagaOrchestrator sut = new VRSagaOrchestrator(this.tickerCacheMock, new ValuationResponseBodyJSONFormatter(), new VRSagaDefaultCircuitBreaker(), this.dataBrokerMock);
        final ValuationReport result = sut.getValuationResponse("DUMMY");
        final String expectedBody = "{\"ticker\":\"DUMMY\",\"discountedCashFlow\":{\"date\":\"2024-09-26\",\"dcf\":15.5,\"stockPrice\":14}}";
        assertEquals(200, result.getStatusCode());
        assertEquals(expectedBody, result.getMessageBody());
        assertEquals("", result.getErrorMessage());
    }

    @Test
    void fmpRandomExceptionWithoutEvenPartialDataShouldProduce500() {
        Mockito.when(this.tickerCacheMock.tickerExists("DUMMY")).thenReturn(true);
        Mockito.when(this.dataBrokerMock.getFromCache("DUMMY")).thenReturn(null);
        Mockito.when(this.dataBrokerMock.getDataFromDb(null,"DUMMY")).thenReturn(null);
        final RuntimeException runtimeException = new RuntimeException("unexpected!");
        final RecordHolder fmpApiRecord = RecordHolder.newRecordHolder("DUMMY", null, null, null, runtimeException);
        Mockito.when(this.dataBrokerMock.getDataFromFmpApi(null, "DUMMY", 2500L)).thenReturn(fmpApiRecord);
        final VRSagaOrchestrator sut = new VRSagaOrchestrator(this.tickerCacheMock, new ValuationResponseBodyJSONFormatter(), new VRSagaDefaultCircuitBreaker(), this.dataBrokerMock);
        final ValuationReport result = sut.getValuationResponse("DUMMY");
        assertEquals(500, result.getStatusCode());
        assertEquals("{\"error\":\"The server encountered an unexpected internal error when trying to generate report for ticker DUMMY!\"}", result.getMessageBody());
        assertEquals("The server encountered an unexpected internal error when trying to generate report for ticker DUMMY!", result.getErrorMessage());
    }
}
