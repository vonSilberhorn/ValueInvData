package com.szilberhornz.valueinvdata.services.stockvaluation.persistence.inmem;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.RecordHolder;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.RecordMapper;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.persistence.api.ValuationDBRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Duration;


public class InMemoryValuationDB implements ValuationDBRepository {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryValuationDB.class);

    //for the sake of simplicity we store our SQL statements here, because we only have a few.
    //should this number grow in the future, we definitely better move them to a resource file instead.
    private static final String QUERY_ALL_DATA_FOR_TICKER = "SELECT * FROM DiscountedCashFlowDb " +
            "LEFT OUTER JOIN PriceTargetSummaryDb ON PriceTargetSummaryDb.Ticker = DiscountedCashFlowDb.Ticker " +
            "LEFT OUTER JOIN PriceTargetConsensusDb ON PriceTargetConsensusDb.Ticker = DiscountedCashFlowDb.Ticker " +
            "WHERE DiscountedCashFlowDb.ticker= ?";

    private static final String INSERT_INTO_DCF = "INSERT INTO DiscountedCashFlowDb VALUES (?, ?, ?, ?)";
    private static final String INSERT_INTO_PTC = "INSERT INTO PriceTargetConsensusDb VALUES (?, ?, ?, ?, ?)";
    private static final String INSERT_INTO_PTS = "INSERT INTO PriceTargetSummaryDb VALUES (?, ?, ?, ?, ?)";


    private final DataSource dataSource;

    public InMemoryValuationDB(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    //In memory is in memory, we don't retry in case of SQL exceptions like we should in case of over the network calls
    @Override
    public RecordHolder queryRecords(String ticker) {
        LOG.info("Starting to query database for the records on ticker {}...", ticker);
        final long start = System.nanoTime();
        try(final Connection conn = dataSource.getConnection(); final PreparedStatement pStmt = conn.prepareStatement(QUERY_ALL_DATA_FOR_TICKER)){
            pStmt.setString(1, ticker);
            final ResultSet resultSet = pStmt.executeQuery();
            final RecordHolder recordHolder = RecordMapper.newRecord(resultSet);
            final long end = System.nanoTime();
            final long durationInMillis = Duration.ofNanos(end - start).toMillis();
            LOG.info("Querying the database for the records on ticker {} took {} milliseconds", ticker, durationInMillis);
            return recordHolder;
        } catch (SQLException sqlException) {
            LOG.error("SQL execution to query for ticker {} failed due to the following reason:", ticker, sqlException);
            return null;
        }
    }

    public static void main(String[] args) {
        DataSource dataSource = new InMemoryDBDataSourceFactory("inMemDbInitializer.txt").getValuationDbDataSource();
        InMemoryValuationDB db = new InMemoryValuationDB(dataSource);
        //System.out.println(db.queryRecords("META"));
        final DiscountedCashFlowDTO dcfDTO = new DiscountedCashFlowDTO("MSN", "2023-09-24", 66.66, 77.77);
        final RecordHolder recordHolder = RecordHolder.newRecordHolder("MSN", dcfDTO, null, null);
        db.insertFullRecord(recordHolder);
        System.out.println(db.queryRecords("MSN").getDiscountedCashFlowDto());
    }

    @Override
    public void insertFullRecord(RecordHolder recordHolder) {
        final DiscountedCashFlowDTO dto = recordHolder.getDiscountedCashFlowDto();
        final Connection conn;
        try {
            conn = this.dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        this.writeDiscountedCashFlowTable(conn, dto);
    }

    private void writeDiscountedCashFlowTable(final Connection connection, final DiscountedCashFlowDTO dto){
        try (final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_INTO_DCF)){
            LOG.info("Writing discounted cashflow data on ticker {} to the database...", dto.ticker());
            final long start = System.nanoTime();
            preparedStatement.setString(1, dto.ticker());
            preparedStatement.setDouble(2, dto.dcf());
            preparedStatement.setString(3, dto.dateString());
            preparedStatement.setDouble(4, dto.stockPrice());
            preparedStatement.executeUpdate();
            final long end = System.nanoTime();
            final long durationInMillis = Duration.ofNanos(end - start).toMillis();
            LOG.info("Writing discounted cashflow data on ticker {} took {} milliseconds", dto.ticker(), durationInMillis);
        } catch (final SQLException sqlException) {
            LOG.error("Persisting data to the DiscountedCashFlowDb failed due to the following reason: ", sqlException);
        }
    }

    private void writePriceTargetSummaryTable(final Connection connection, final PriceTargetSummaryDTO dto){
        try (final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_INTO_PTS)){
            LOG.info("Writing price target summary data on ticker {} to the database...", dto.ticker());
            final long start = System.nanoTime();
            preparedStatement.setString(1, dto.ticker());
            preparedStatement.setInt(2, dto.lastMonth());
            preparedStatement.setDouble(3, dto.lastMonthAvgPriceTarget());
            preparedStatement.setInt(4, dto.lastQuarter());
            preparedStatement.setDouble(5, dto.lastQuarterAvgPriceTarget());
            preparedStatement.executeUpdate();
            final long end = System.nanoTime();
            final long durationInMillis = Duration.ofNanos(end - start).toMillis();
            LOG.info("Writing price target summary data on ticker {} took {} milliseconds", dto.ticker(), durationInMillis);
        } catch (final SQLException sqlException) {
            LOG.error("Persisting data to the PriceTargetSummaryDb failed due to the following reason: ", sqlException);
        }
    }

    private void writePriceTargetConsensusTable(final Connection connection, final PriceTargetConsensusDTO dto){
        try (final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_INTO_PTC)){
            LOG.info("Writing price target consensus data on ticker {} to the database...", dto.ticker());
            final long start = System.nanoTime();
            preparedStatement.setString(1, dto.ticker());
            preparedStatement.setDouble(2, dto.targetHigh());
            preparedStatement.setDouble(3, dto.targetLow());
            preparedStatement.setDouble(4, dto.targetConsensus());
            preparedStatement.setDouble(5, dto.targetMedian());
            preparedStatement.executeUpdate();
            final long end = System.nanoTime();
            final long durationInMillis = Duration.ofNanos(end - start).toMillis();
            LOG.info("Writing price target consensus data on ticker {} took {} milliseconds", dto.ticker(), durationInMillis);
        } catch (final SQLException sqlException) {
            LOG.error("Persisting data to the PriceTargetConsensusDb failed due to the following reason: ", sqlException);
        }
    }
}
