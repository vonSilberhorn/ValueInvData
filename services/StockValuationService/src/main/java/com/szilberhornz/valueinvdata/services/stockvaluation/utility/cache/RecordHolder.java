package com.szilberhornz.valueinvdata.services.stockvaluation.utility.cache;

import com.szilberhornz.valueinvdata.services.stockvaluation.model.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.model.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.model.record.PriceTargetSummaryDTO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class is designed to prevent outside entities to change the state of its instances: e.g. a class looking up
 * cached items should be able to see what is inside the item, but changes should only be made within the
 * {@link com.szilberhornz.valueinvdata.services.stockvaluation.cache} package
 * Classes outside the cache package may construct new RecordHolders though, and also inspect their inner state, just not change it
 * <p/>
 * No need to synchronize this class due to the fact that the ConcurrentHashMap in the cache locks the bucket when performing write operations
 */
public class RecordHolder {

    private final String ticker;

    private DiscountedCashFlowDTO discountedCashFlowDto;
    private PriceTargetConsensusDTO priceTargetConsensusDto;
    private PriceTargetSummaryDTO priceTargetSummaryDto;

    private Throwable causeOfNullDtos = null;

    private RecordHolder(final String ticker, final DiscountedCashFlowDTO discountedCashFlowDto, final PriceTargetConsensusDTO priceTargetConsensusDto, final PriceTargetSummaryDTO priceTargetSummaryDto) {
        this.ticker = ticker;
        this.discountedCashFlowDto = discountedCashFlowDto;
        this.priceTargetConsensusDto = priceTargetConsensusDto;
        this.priceTargetSummaryDto = priceTargetSummaryDto;
    }

    private RecordHolder(final String ticker, final DiscountedCashFlowDTO discountedCashFlowDto, final PriceTargetConsensusDTO priceTargetConsensusDto,
                         final PriceTargetSummaryDTO priceTargetSummaryDto, final Throwable causeOfNullDtos) {
        this.ticker = ticker;
        this.discountedCashFlowDto = discountedCashFlowDto;
        this.priceTargetConsensusDto = priceTargetConsensusDto;
        this.priceTargetSummaryDto = priceTargetSummaryDto;
        this.causeOfNullDtos = causeOfNullDtos;
    }

    //we allow outside entities to get an immutable instance - immutable for them as they don't see the accessors
    public static RecordHolder newRecordHolder(@NotNull final String ticker, @Nullable final DiscountedCashFlowDTO dcfDto, @Nullable final PriceTargetConsensusDTO ptcDto, @Nullable final PriceTargetSummaryDTO ptsDto){
        return new RecordHolder(ticker, dcfDto, ptcDto, ptsDto);
    }

    public static RecordHolder newRecordHolder(@NotNull final String ticker, @Nullable final DiscountedCashFlowDTO dcfDto,
                                               @Nullable final PriceTargetConsensusDTO ptcDto, @Nullable final PriceTargetSummaryDTO ptsDto,
                                               final Throwable causeOfNullDtos){
        return new RecordHolder(ticker, dcfDto, ptcDto, ptsDto, causeOfNullDtos);
    }

    public int getDtoCount(){
        int result = 0;
        if (this.discountedCashFlowDto != null) {
            result++;
        }
        if (this.priceTargetConsensusDto != null) {
            result++;
        }
        if (this.priceTargetSummaryDto != null) {
            result++;
        }
        return result;
    }


    public boolean isDataMissing(){
        return this.discountedCashFlowDto == null
                || this.priceTargetSummaryDto == null
                || this.priceTargetConsensusDto == null;
    }

    public Throwable getCauseOfNullDtos(){
        return this.causeOfNullDtos;
    }

    //we don't allow anyone to construct this object outside the actual cache implementors
    RecordHolder(final String ticker) {
        this.ticker = ticker;
    }

    @NotNull
    public String getTicker() {
        return this.ticker;
    }

    @Nullable
    public DiscountedCashFlowDTO getDiscountedCashFlowDto() {
        return this.discountedCashFlowDto;
    }


    //we don't allow anyone to access setters outside the actual cache package
    void setDiscountedCashFlowDto(final DiscountedCashFlowDTO discountedCashFlowDto) {
        this.discountedCashFlowDto = discountedCashFlowDto;
    }

    @Nullable
    public PriceTargetConsensusDTO getPriceTargetConsensusDto() {
        return this.priceTargetConsensusDto;
    }

    //we don't allow anyone to access setters outside the actual cache package
    void setPriceTargetConsensusDto(final PriceTargetConsensusDTO priceTargetConsensusDto) {
        this.priceTargetConsensusDto = priceTargetConsensusDto;
    }

    @Nullable
    public PriceTargetSummaryDTO getPriceTargetSummaryDto() {
        return this.priceTargetSummaryDto;
    }

    //we don't allow anyone to access setters outside the actual cache package
    void setPriceTargetSummaryDto(final PriceTargetSummaryDTO priceTargetSummaryDto) {
        this.priceTargetSummaryDto = priceTargetSummaryDto;
    }
}
