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
import java.util.concurrent.atomic.AtomicReference;

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
    public RecordHolder getDataFromFmpApi(@Nullable final RecordHolder recordFromDb, final String ticker, final long timeOutInMillis) {
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
        //capture exceptions, if any, and return it along with data. This is necessary because we may have partial success
        // and may be getting exceptions for other parts at the same time! We don't want to dismiss valid data just because
        // one of the api calls threw an exception!
        final AtomicReference<Throwable> fmpApiCallFailure = new AtomicReference<>();
        try {
            //we have to block before returning to scrape all the missing data we can
            dcfDto = dcfDto == null ? dcfDtoFuture.exceptionally( throwable -> {
                fmpApiCallFailure.set(throwable);
                return null;
            }).completeOnTimeout(null, timeOutInMillis, TimeUnit.MILLISECONDS).get() : dcfDto;
            ptsDto = ptsDto == null ? ptsDtoFuture.exceptionally( throwable -> {
                fmpApiCallFailure.set(throwable);
                return null;
            }).completeOnTimeout(null, timeOutInMillis, TimeUnit.MILLISECONDS).get() : ptsDto;
            ptcDto = ptcDto == null ? ptcDtoFuture.exceptionally( throwable -> {
                fmpApiCallFailure.set(throwable);
                return null;
            }).completeOnTimeout(null, timeOutInMillis, TimeUnit.MILLISECONDS).get() : ptcDto;
        } catch (final InterruptedException interruptedException) {
            LOG.error("Unexpected interruption while getting data from the FMP api for ticker {}!", ticker, interruptedException);
            Thread.currentThread().interrupt();
        } catch (final ExecutionException executionException) {
            fmpApiCallFailure.set(executionException.getCause());
        }
        //as this is the last step, we return what we have, even if it's all null
        if (fmpApiCallFailure.get() != null) {
            //the throwable is always an ExecutionException, we are interested in its cause
            return RecordHolder.newRecordHolder(ticker, dcfDto, ptcDto, ptsDto, fmpApiCallFailure.get().getCause());
        } else {
            return RecordHolder.newRecordHolder(ticker, dcfDto, ptcDto, ptsDto);
        }
    }

    public void persistData(final String ticker, final RecordHolder recordFromCache, final RecordHolder recordFromDb, final RecordHolder recordFromFmpApi) {
        //the api data is a superset of the others
        if (recordFromFmpApi != null) {
            this.addToCache(ticker, recordFromCache, recordFromFmpApi);
            this.writeToDb(ticker, recordFromDb, recordFromFmpApi);
            //write records from db to cache
        } else if (recordFromDb != null) {
            this.addToCache(ticker, recordFromCache, recordFromDb);
        }
    }

    private void writeToDb(final String ticker, final RecordHolder recordFromDb, final RecordHolder recordFromFmpApi) {
        if (recordFromDb == null) {
            LOG.info("Writing {} ticker data to the database!", ticker);
            this.valuationDbRepository.insertFullRecord(recordFromFmpApi);
        } else if (recordFromDb.isDataMissing() && recordFromFmpApi.getDtoCount() > recordFromDb.getDtoCount()) {
            if (recordFromDb.getDiscountedCashFlowDto() == null && recordFromFmpApi.getDiscountedCashFlowDto() != null){
                LOG.info("Writing discounted cashflow {} ticker data to the database!", ticker);
                this.valuationDbRepository.insertDiscountedCashFlowData(recordFromFmpApi.getDiscountedCashFlowDto());
            }
            if (recordFromDb.getPriceTargetConsensusDto() == null && recordFromFmpApi.getPriceTargetConsensusDto() != null){
                LOG.info("Writing price target consensus {} ticker data to the database!", ticker);
                this.valuationDbRepository.insertPriceTargetConsensusData(recordFromFmpApi.getPriceTargetConsensusDto());
            }
            if (recordFromDb.getPriceTargetSummaryDto() == null && recordFromFmpApi.getPriceTargetSummaryDto() != null){
                LOG.info("Writing price target summery {} ticker data to the database!", ticker);
                this.valuationDbRepository.insertPriceTargetSummaryData(recordFromFmpApi.getPriceTargetSummaryDto());
            }
        }
    }

    private void addToCache(final String ticker, final RecordHolder recordFromCache, final RecordHolder superSet){
        if (recordFromCache == null){
            LOG.info("Adding full {} ticker data to the cache!", ticker);
            this.valuationServerCache.put(ticker, superSet.getDiscountedCashFlowDto());
            this.valuationServerCache.put(ticker, superSet.getPriceTargetConsensusDto());
            this.valuationServerCache.put(ticker, superSet.getPriceTargetSummaryDto());
        } else if (recordFromCache.isDataMissing()){
            if (recordFromCache.getDiscountedCashFlowDto() == null) {
                LOG.info("Adding discounted cashflow {} ticker data to the cache!", ticker);
                this.valuationServerCache.put(ticker, superSet.getDiscountedCashFlowDto());
            }
            if (recordFromCache.getPriceTargetConsensusDto() == null) {
                LOG.info("Adding price target consensus {} ticker data to the cache!", ticker);
                this.valuationServerCache.put(ticker, superSet.getPriceTargetConsensusDto());
            }
            if (recordFromCache.getPriceTargetSummaryDto() == null) {
                LOG.info("Adding price target summary {} ticker data to the cache!", ticker);
                this.valuationServerCache.put(ticker, superSet.getPriceTargetSummaryDto());
            }
        }
    }
}
