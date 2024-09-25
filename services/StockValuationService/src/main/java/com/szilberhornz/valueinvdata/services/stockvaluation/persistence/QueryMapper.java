package com.szilberhornz.valueinvdata.services.stockvaluation.persistence;

import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static com.szilberhornz.valueinvdata.services.stockvaluation.persistence.TSQLSyntax.*;

public final class QueryMapper {

    public static PreparedStatement prepareDiscountedCashFlowInsert(final Connection connection, final DiscountedCashFlowDTO dto) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_INTO_DCF);
        preparedStatement.setString(1, dto.ticker());
        preparedStatement.setDouble(2, dto.dcf());
        preparedStatement.setString(3, dto.dateString());
        preparedStatement.setDouble(4, dto.stockPrice());
        return preparedStatement;
    }

    public static PreparedStatement preparePriceTargetSummaryInsert(final Connection connection, final PriceTargetSummaryDTO dto) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_INTO_PTS);
        preparedStatement.setString(1, dto.ticker());
        preparedStatement.setInt(2, dto.lastMonth());
        preparedStatement.setDouble(3, dto.lastMonthAvgPriceTarget());
        preparedStatement.setInt(4, dto.lastQuarter());
        preparedStatement.setDouble(5, dto.lastQuarterAvgPriceTarget());
        return preparedStatement;
    }

    public static PreparedStatement preparePriceTargetConsensusInsert(final Connection connection, final PriceTargetConsensusDTO dto) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_INTO_PTC);
        preparedStatement.setString(1, dto.ticker());
        preparedStatement.setDouble(2, dto.targetHigh());
        preparedStatement.setDouble(3, dto.targetLow());
        preparedStatement.setDouble(4, dto.targetConsensus());
        preparedStatement.setDouble(5, dto.targetMedian());
        return preparedStatement;
    }

    public static PreparedStatement prepareQueryForAllRecordsOnTicker(final Connection connection, final String ticker) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement(QUERY_ALL_DATA_FOR_TICKER);
        preparedStatement.setString(1, ticker);
        return preparedStatement;
    }

    public static PreparedStatement prepareQueryForDiscountedCashFlowData(final Connection connection, final String ticker) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement(SELECT_FROM_DCF);
        preparedStatement.setString(1, ticker);
        return preparedStatement;
    }

    public static PreparedStatement prepareQueryForPriceTargetSummaryData(final Connection connection, final String ticker) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement(SELECT_FROM_PTS);
        preparedStatement.setString(1, ticker);
        return preparedStatement;
    }

    public static PreparedStatement prepareQueryForPriceTargetConsensusData(final Connection connection, final String ticker) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement(SELECT_FROM_PTC);
        preparedStatement.setString(1, ticker);
        return preparedStatement;
    }

    private QueryMapper(){
        //no need to instantiate
    }
}
