package com.szilberhornz.valueinvdata.services.stockvaluation.core;

import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;

public final class RecordMapper {

    private static final Logger LOG = LoggerFactory.getLogger(RecordMapper.class);

    @Nullable
    public static DiscountedCashFlowDTO newDcfDto(final HttpResponse<String> response) {
        try {
            JSONArray array = new JSONArray(response.body());
            JSONObject object = array.getJSONObject(0);
            return new DiscountedCashFlowDTO(object.getString("symbol"),
                    object.getString("date"), object.getDouble("dcf"), object.getDouble("Stock Price"));
        } catch (RuntimeException exception) {
            LOG.error("Unexpected error happened while parsing HttpResponse to DcfDto. The response body is: {}", response.body(), exception);
            return null;
        }
    }

    public static PriceTargetConsensusDTO newPtcDto(final HttpResponse<String> response){
        try {
            JSONArray array = new JSONArray(response.body());
            JSONObject object = array.getJSONObject(0);
            return new PriceTargetConsensusDTO(object.getString("symbol"),
                    object.getDouble("targetHigh"), object.getDouble("targetLow"), object.getDouble("targetConsensus"), object.getDouble("targetMedian"));
        } catch (RuntimeException exception) {
            LOG.error("Unexpected error happened while parsing HttpResponse to PtcDto. The response body is: {}", response.body(), exception);
            return null;
        }
    }

    public static PriceTargetSummaryDTO newPtsDto(final HttpResponse<String> response){
        try {
            JSONArray array = new JSONArray(response.body());
            JSONObject object = array.getJSONObject(0);
            return new PriceTargetSummaryDTO(object.getString("symbol"),
                    object.getInt("lastMonth"), object.getDouble("lastMonthAvgPriceTarget"), object.getInt("lastQuarter"), object.getDouble("lastQuarterAvgPriceTarget"));
        } catch (RuntimeException exception) {
            LOG.error("Unexpected error happened while parsing HttpResponse to PtsDto. The response body is: {}", response.body(), exception);
            return null;
        }
    }

    private RecordMapper() {
        //static helper class, no need to instantiate
    }
}
