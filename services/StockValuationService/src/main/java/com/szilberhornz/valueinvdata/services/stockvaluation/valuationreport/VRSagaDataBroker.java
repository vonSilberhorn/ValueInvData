package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.RecordHolder;
import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationServerCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.fmp.FMPResponseHandler;
import com.szilberhornz.valueinvdata.services.stockvaluation.persistence.api.ValuationDBRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class VRSagaDataBroker {

    private static final Logger LOG = LoggerFactory.getLogger(VRSagaDataBroker.class);

    private final ValuationDBRepository valuationDbRepository;
    private final ValuationServerCache valuationServerCache;
    private final FMPResponseHandler fmpResponseHandler;

    public VRSagaDataBroker(final ValuationDBRepository valuationDbRepository, final ValuationServerCache valuationServerCache,
                            final FMPResponseHandler fmpResponseHandler) {
        this.valuationDbRepository = valuationDbRepository;
        this.valuationServerCache = valuationServerCache;
        this.fmpResponseHandler = fmpResponseHandler;
    }

    @Nullable
    public RecordHolder getFromCache(final String ticker){
        return this.valuationServerCache.get(ticker);
    }

    @Nullable
    public RecordHolder getDataFromDb(@Nullable final RecordHolder recordFromCache, final String ticker){
        if (recordFromCache == null){
            return this.valuationDbRepository.queryRecords(ticker);
        } else if (recordFromCache.isDataMissing()){
            DiscountedCashFlowDTO dcfDto = recordFromCache.getDiscountedCashFlowDto();
            PriceTargetSummaryDTO ptsDto = recordFromCache.getPriceTargetSummaryDto();
            PriceTargetConsensusDTO ptcDto = recordFromCache.getPriceTargetConsensusDto();
            if (dcfDto == null){
                dcfDto = this.valuationDbRepository.queryDiscountedCashFlowData(ticker);
            }
            if (ptsDto == null) {
                ptsDto = this.valuationDbRepository.queryPriceTargetSummaryData(ticker);
            }
            if (ptcDto == null){
                ptcDto = this.valuationDbRepository.queryPriceTargetConsensusData(ticker);
            }
            return RecordHolder.newRecordHolder(ticker, dcfDto, ptcDto, ptsDto);
        }
        return recordFromCache;
    }


    @NotNull
    public RecordHolder getDataFromFmpApi(@Nullable final RecordHolder recordFromDb, final String ticker, final long timeOutInSeconds) throws Throwable {
        DiscountedCashFlowDTO dcfDto = null;
        PriceTargetSummaryDTO ptsDto = null;
        PriceTargetConsensusDTO ptcDto = null;
        if (recordFromDb != null){
            dcfDto = recordFromDb.getDiscountedCashFlowDto();
            ptsDto = recordFromDb.getPriceTargetSummaryDto();
            ptcDto = recordFromDb.getPriceTargetConsensusDto();
        }
        CompletableFuture<DiscountedCashFlowDTO> dcfDtoFuture = null;
        CompletableFuture<PriceTargetSummaryDTO> ptsDtoFuture = null;
        CompletableFuture<PriceTargetConsensusDTO> ptcDtoFuture = null;
        //start the missing ones asynchronously
        if (dcfDto == null){
            dcfDtoFuture = CompletableFuture.supplyAsync(()->this.fmpResponseHandler.getDiscountedCashFlowReportFromFmpApi(ticker));
        }
        if (ptsDto == null) {
            ptsDtoFuture = CompletableFuture.supplyAsync(()->this.fmpResponseHandler.getPriceTargetSummaryReportFromFmpApi(ticker));
        }
        if (ptcDto == null){
            ptcDtoFuture = CompletableFuture.supplyAsync(()->this.fmpResponseHandler.getPriceTargetConsensusReportFromFmpApi(ticker));
        }
        try {
            //but we have to block before returning to scrape all the missing data we can
            dcfDto = dcfDto == null ? dcfDtoFuture.get(timeOutInSeconds, TimeUnit.MILLISECONDS) : dcfDto;
            ptsDto = ptsDto == null ? ptsDtoFuture.get(timeOutInSeconds, TimeUnit.MILLISECONDS) : ptsDto;
            ptcDto = ptcDto == null ? ptcDtoFuture.get(timeOutInSeconds, TimeUnit.MILLISECONDS) : ptcDto;
        } catch (final InterruptedException interruptedException) {
            LOG.error("Unexpected interruption while getting data from the FMP api for ticker {}!", ticker, interruptedException);
            Thread.currentThread().interrupt();
        } catch (final ExecutionException executionException) {
            //we must throw the cause of the execution exception because it may be api key issue
            throw executionException.getCause();
        } catch (final TimeoutException timeoutException) {
            LOG.error("Circuit breaker timeout while trying to get data from the FMP api for ticker {}!", ticker, timeoutException);
        }
        //as this is the last step, we return what we have, even if it's all null
        return RecordHolder.newRecordHolder(ticker, dcfDto, ptcDto, ptsDto);
    }

    public void persistData(final String ticker, final RecordHolder recordFromCache, final RecordHolder recordFromDb, final RecordHolder recordFromFmpApi) {
        //the api data is a superset of the others
        if (recordFromFmpApi != null) {
            final DiscountedCashFlowDTO dcfDto = recordFromFmpApi.getDiscountedCashFlowDto();
            final PriceTargetConsensusDTO ptcDto = recordFromFmpApi.getPriceTargetConsensusDto();
            final PriceTargetSummaryDTO ptsDto = recordFromFmpApi.getPriceTargetSummaryDto();
            final RecordHolder temp = RecordHolder.newRecordHolder(ticker, dcfDto, ptcDto, ptsDto);
            if (recordFromDb == null || recordFromDb.isDataMissing()) {
                LOG.info("Writing {} ticker data to the database!", ticker);
                this.valuationDbRepository.insertFullRecord(temp);
            }
            if (recordFromCache == null){
                LOG.info("Adding full {} ticker data to the cache!", ticker);
                this.valuationServerCache.put(ticker, dcfDto);
                this.valuationServerCache.put(ticker, ptcDto);
                this.valuationServerCache.put(ticker, ptsDto);
            } else if (recordFromCache.isDataMissing()){
                if (recordFromCache.getDiscountedCashFlowDto() == null) {
                    LOG.info("Adding discounted cashflow {} ticker data to the cache!", ticker);
                    this.valuationServerCache.put(ticker, dcfDto);
                }
                if (recordFromCache.getPriceTargetConsensusDto() == null) {
                    LOG.info("Adding price target consensus {} ticker data to the cache!", ticker);
                    this.valuationServerCache.put(ticker, ptcDto);
                }
                if (recordFromCache.getPriceTargetSummaryDto() == null) {
                    LOG.info("Adding price target summary {} ticker data to the cache!", ticker);
                    this.valuationServerCache.put(ticker, ptsDto);
                }
            }
        }
    }
}
