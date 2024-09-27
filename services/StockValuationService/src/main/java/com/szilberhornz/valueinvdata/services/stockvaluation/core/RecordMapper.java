package com.szilberhornz.valueinvdata.services.stockvaluation.core;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.RecordHolder;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;
import org.jetbrains.annotations.NotNull;
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

/**
 * Mapping HttpResponses from the FMP api and ResultSets from the database on our record types.
 * <p>
 * The mapping from ResultSets corresponds to how the MSSQL driver translates MSSQL server types to Java types
 * as described here: <a href="https://learn.microsoft.com/en-us/sql/language-extensions/how-to/java-to-sql-data-types?view=sql-server-ver16">MSSQL Java types</a>
 */
public final class RecordMapper {

    private static final Logger LOG = LoggerFactory.getLogger(RecordMapper.class);

    @Nullable
    public static DiscountedCashFlowDTO newDcfDto(final HttpResponse<String> response) {
        try {
            final JSONArray array = new JSONArray(response.body());
            final JSONObject object = array.getJSONObject(0);
            return new DiscountedCashFlowDTO(object.getString("symbol"),
                    object.getString("date"), object.getDouble("dcf"), object.getDouble("Stock Price"));
        } catch (final RuntimeException exception) {
            LOG.error("Unexpected error happened while parsing HttpResponse to DcfDto. The response body is: {}", response.body(), exception);
            return null;
        }
    }

    public static PriceTargetConsensusDTO newPtcDto(final HttpResponse<String> response){
        try {
            final JSONArray array = new JSONArray(response.body());
            final JSONObject object = array.getJSONObject(0);
            return new PriceTargetConsensusDTO(object.getString("symbol"),
                    object.getDouble("targetHigh"), object.getDouble("targetLow"), object.getDouble("targetConsensus"), object.getDouble("targetMedian"));
        } catch (final RuntimeException exception) {
            LOG.error("Unexpected error happened while parsing HttpResponse to PtcDto. The response body is: {}", response.body(), exception);
            return null;
        }
    }

    public static PriceTargetSummaryDTO newPtsDto(final HttpResponse<String> response){
        try {
            final JSONArray array = new JSONArray(response.body());
            final JSONObject object = array.getJSONObject(0);
            return new PriceTargetSummaryDTO(object.getString("symbol"),
                    object.getInt("lastMonth"), object.getDouble("lastMonthAvgPriceTarget"), object.getInt("lastQuarter"), object.getDouble("lastQuarterAvgPriceTarget"));
        } catch (final RuntimeException exception) {
            LOG.error("Unexpected error happened while parsing HttpResponse to PtsDto. The response body is: {}", response.body(), exception);
            return null;
        }
    }

    @Nullable
    public static RecordHolder newRecord(final ResultSet resultSet) throws SQLException {
        final int expectedSize = 14;
        final List<Object> tempList = listFromResultSet(resultSet, expectedSize);
        return tempList.isEmpty() ? null : constructRecord(tempList);
    }

    @Nullable
    public static DiscountedCashFlowDTO newDcfDto(final ResultSet resultSet) throws SQLException {
        final int expectedSize = 4;
        final List<Object> tempList = listFromResultSet(resultSet, expectedSize);
        return tempList.isEmpty() ? null : constructDiscountedCashFlowDTO(tempList);
    }

    @Nullable
    public static PriceTargetConsensusDTO newPtcDto(final ResultSet resultSet) throws SQLException {
        final int expectedSize = 5;
        final List<Object> tempList = listFromResultSet(resultSet, expectedSize);
        return tempList.isEmpty() ? null : constructPriceTargetConsensusDTO(tempList);
    }

    @Nullable
    public static PriceTargetSummaryDTO newPtsDto(final ResultSet resultSet) throws SQLException {
        final int expectedSize = 5;
        final List<Object> tempList = listFromResultSet(resultSet, expectedSize);
        return tempList.isEmpty() ? null : constructPriceTargetSummaryDTO(tempList);
    }

    @NotNull
    private static List<Object> listFromResultSet(final ResultSet resultSet, final int expectedSize) throws SQLException {
        final List<Object> result = new ArrayList<>(expectedSize);
        final ResultSetMetaData metaData = resultSet.getMetaData();
        final int columnCount = metaData.getColumnCount();
        while (resultSet.next()) {
            for (int i = 1; i <= columnCount; i++) {
                result.add(resultSet.getObject(i));
            }
        }
        if (result.size() > expectedSize) {
            //this is to make sure we don't go full Jurassic Park and fail to notice that we have more items than we should!
            throw new IllegalStateException("The ResultSet unexpectedly held more than one row of data! This should " +
                    "not have happened as the ticker is the primary key in the db tables and we only have " + expectedSize + " columns!");
        }
        return result;
    }

    @Nullable
    private static RecordHolder constructRecord(final List<Object> dataList) {
        final List<Object> dcfSublist = dataList.subList(0, 4);
        final DiscountedCashFlowDTO dcfDto = constructDiscountedCashFlowDTO(dcfSublist);
        final List<Object> ptsSubList = dataList.subList(4, 9);
        final PriceTargetSummaryDTO ptsDto = constructPriceTargetSummaryDTO(ptsSubList);
        final List<Object> ptcSubList = dataList.subList(9, 14);
        final PriceTargetConsensusDTO ptcDto = constructPriceTargetConsensusDTO(ptcSubList);
        if (dcfDto == null && ptsDto == null && ptcDto == null){
            return null;
        } else {
            final String ticker = getTicker(dcfDto, ptsDto, ptcDto);
            return RecordHolder.newRecordHolder(ticker, dcfDto, ptcDto, ptsDto);
        }
    }

    @NotNull
    private static String getTicker(final DiscountedCashFlowDTO dcfDto, final PriceTargetSummaryDTO ptsDto, final PriceTargetConsensusDTO ptcDto) {
        if (dcfDto != null) {
            return dcfDto.ticker();
        } else if (ptcDto != null){
            return ptcDto.ticker();
        } else {
            return ptsDto.ticker();
        }
    }

    @Nullable
    private static PriceTargetConsensusDTO constructPriceTargetConsensusDTO(final List<Object> ptcSubList) {
        final int ptcNullCount = (int) ptcSubList.stream().filter(Objects::isNull).count();
        PriceTargetConsensusDTO ptcDto = null;
        if (ptcNullCount == 0){
            ptcDto = new PriceTargetConsensusDTO(
                    (String) ptcSubList.getFirst(),
                    ((BigDecimal) ptcSubList.get(1)).doubleValue(),
                    ((BigDecimal) ptcSubList.get(2)).doubleValue(),
                    ((BigDecimal) ptcSubList.get(3)).doubleValue(),
                    ((BigDecimal) ptcSubList.getLast()).doubleValue()
            );
        }
        return ptcDto;
    }

    @Nullable
    private static PriceTargetSummaryDTO constructPriceTargetSummaryDTO(final List<Object> ptsSubList) {
        final int ptsNullCount = (int) ptsSubList.stream().filter(Objects::isNull).count();
        PriceTargetSummaryDTO ptsDto = null;
        if (ptsNullCount == 0) {
            ptsDto = new PriceTargetSummaryDTO(
                    (String) ptsSubList.getFirst(),
                    (int) ptsSubList.get(1),
                    ((BigDecimal) ptsSubList.get(2)).doubleValue(),
                    (int) ptsSubList.get(3),
                    ((BigDecimal) ptsSubList.getLast()).doubleValue()
            );
        }
        return ptsDto;
    }

    @Nullable
    private static DiscountedCashFlowDTO constructDiscountedCashFlowDTO(final List<Object> dcfSublist) {
        final int dcfNullCount = (int) dcfSublist.stream().filter(Objects::isNull).count();
        DiscountedCashFlowDTO dcfDto = null;
        if (dcfNullCount == 0) {
            dcfDto = new DiscountedCashFlowDTO(
                    (String) dcfSublist.getFirst(),
                    ((Date) dcfSublist.get(2)).toString(),
                    ((BigDecimal) dcfSublist.get(1)).doubleValue(),
                    ((BigDecimal) dcfSublist.getLast()).doubleValue()
            );
        }
        return dcfDto;
    }

    private RecordMapper() {
        //static helper class, no need to instantiate
    }
}
