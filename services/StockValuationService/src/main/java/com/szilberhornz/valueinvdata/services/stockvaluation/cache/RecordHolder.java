package com.szilberhornz.valueinvdata.services.stockvaluation.cache;

import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class is designed to prevent outside entities to change the state of its instances: e.g. a class looking up
 * cached items should be able to see what is inside the item, but changes should only be made within the
 * {@link com.szilberhornz.valueinvdata.services.stockvaluation.cache} package
 * <p/>
 * No need to synchronize this class due to the fact that the ConcurrentHashMap in the cache locks the bucket when performing write operations
 */
public class RecordHolder {

    private final String ticker;

    private DiscountedCashFlowDTO discountedCashFlowDto;
    private PriceTargetConsensusDTO priceTargetConsensusDto;
    private PriceTargetSummaryDTO priceTargetSummaryDto;

    private RecordHolder(String ticker, DiscountedCashFlowDTO discountedCashFlowDto, PriceTargetConsensusDTO priceTargetConsensusDto, PriceTargetSummaryDTO priceTargetSummaryDto) {
        this.ticker = ticker;
        this.discountedCashFlowDto = discountedCashFlowDto;
        this.priceTargetConsensusDto = priceTargetConsensusDto;
        this.priceTargetSummaryDto = priceTargetSummaryDto;
    }

    //we allow outside entities to get an immutable instance - immutable for them as they don't see the accessors
    public static RecordHolder newRecordHolder(@NotNull String ticker, @Nullable DiscountedCashFlowDTO dcfDto, @Nullable PriceTargetConsensusDTO ptcDto, @Nullable PriceTargetSummaryDTO ptsDto){
        return new RecordHolder(ticker, dcfDto, ptcDto, ptsDto);
    }

    //we don't allow anyone to construct this object outside the actual cache implementors
    RecordHolder(String ticker) {
        this.ticker = ticker;
    }

    @NotNull
    public String getTicker() {
        return ticker;
    }

    @Nullable
    public DiscountedCashFlowDTO getDiscountedCashFlowDto() {
        return discountedCashFlowDto;
    }


    //we don't allow anyone to access setters outside the actual cache package
    void setDiscountedCashFlowDto(DiscountedCashFlowDTO discountedCashFlowDto) {
        this.discountedCashFlowDto = discountedCashFlowDto;
    }

    @Nullable
    public PriceTargetConsensusDTO getPriceTargetConsensusDto() {
        return priceTargetConsensusDto;
    }

    //we don't allow anyone to access setters outside the actual cache package
    void setPriceTargetConsensusDto(PriceTargetConsensusDTO priceTargetConsensusDto) {
        this.priceTargetConsensusDto = priceTargetConsensusDto;
    }

    @Nullable
    public PriceTargetSummaryDTO getPriceTargetSummaryDto() {
        return priceTargetSummaryDto;
    }

    //we don't allow anyone to access setters outside the actual cache package
    void setPriceTargetSummaryDto(PriceTargetSummaryDTO priceTargetSummaryDto) {
        this.priceTargetSummaryDto = priceTargetSummaryDto;
    }
}
