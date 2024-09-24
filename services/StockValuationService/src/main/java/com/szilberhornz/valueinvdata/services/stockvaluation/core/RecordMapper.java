package com.szilberhornz.valueinvdata.services.stockvaluation.core;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.RecordHolder;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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

    @Nullable
    public static RecordHolder newRecord(final ResultSet resultSet) throws SQLException {
        final List<Object> tempList = new ArrayList<>(14);
        final ResultSetMetaData metaData = resultSet.getMetaData();
        final int columnCount = metaData.getColumnCount();
        while (resultSet.next()) {
            for (int i = 1; i <= columnCount; i++) {
                tempList.add(resultSet.getObject(i));
            }
        }
        if (tempList.size() > 14) {
            //this is to make sure we don't go full Jurassic Park and fail to notice that we have more items than we should!
            throw new IllegalStateException("The ResultSet unexpectedly held more than one row of data! This should " +
                    "not have happened as the ticker is the primary key in the db tables and we only have 14 columns!");
        }
        if (tempList.isEmpty()){
            return null;
        } else {
            return constructRecord(tempList);
        }
    }

    @Nullable
    private static RecordHolder constructRecord(final List<Object> dataList) {
        DiscountedCashFlowDTO dcfDto = null;
        PriceTargetSummaryDTO ptsDto = null;
        PriceTargetConsensusDTO ptcDto = null;
        String ticker = null;
        List<Object> dcfSublist = dataList.subList(0, 4);
        int dcfNullCount = (int) dcfSublist.stream().filter(Objects::isNull).count();
        if (dcfNullCount == 0) {
            ticker = (String) dcfSublist.getFirst();
            dcfDto = new DiscountedCashFlowDTO(
                    (String) dcfSublist.getFirst(),
                    ((Date) dcfSublist.get(2)).toString(),
                    ((BigDecimal) dcfSublist.get(1)).doubleValue(),
                    ((BigDecimal) dcfSublist.getLast()).doubleValue()
            );
        }
        List<Object> ptsSubList = dataList.subList(4, 9);
        int ptsNullCount = (int) ptsSubList.stream().filter(Objects::isNull).count();
        if (ptsNullCount == 0) {
            if (ticker == null) {
                ticker = (String) ptsSubList.getFirst();
            }
            ptsDto = new PriceTargetSummaryDTO(
                    (String) ptsSubList.getFirst(),
                    (int) ptsSubList.get(1),
                    ((BigDecimal) ptsSubList.get(2)).doubleValue(),
                    (int) ptsSubList.get(3),
                    ((BigDecimal) ptsSubList.getLast()).doubleValue()
            );
        }
        List<Object> ptcSubList = dataList.subList(9, 14);
        int ptcNullCount = (int) ptcSubList.stream().filter(Objects::isNull).count();
        if (ptcNullCount == 0){
            if (ticker == null) {
                ticker = (String) ptcSubList.getFirst();
            }
            ptcDto = new PriceTargetConsensusDTO(
                    (String) ptcSubList.getFirst(),
                    ((BigDecimal) ptcSubList.get(1)).doubleValue(),
                    ((BigDecimal) ptcSubList.get(2)).doubleValue(),
                    ((BigDecimal) ptcSubList.get(3)).doubleValue(),
                    ((BigDecimal) ptcSubList.getLast()).doubleValue()
            );
        }
        if (dcfDto == null && ptsDto == null && ptcDto == null){
            return null;
        } else {
            return RecordHolder.newRecordHolder(ticker, dcfDto, ptcDto, ptsDto);
        }
    }

    private RecordMapper() {
        //static helper class, no need to instantiate
    }
}
