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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


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
        final PriceTargetSummaryDTO ptsDTO = new PriceTargetSummaryDTO("MSN", 5, 11.1, 5, 11.2);
        final PriceTargetConsensusDTO ptcDTO = new PriceTargetConsensusDTO("MSN", 15, 10, 15,12);
        final RecordHolder recordHolder = RecordHolder.newRecordHolder("MSN", dcfDTO, ptcDTO, ptsDTO);
        db.insertFullRecord(recordHolder);
        System.out.println(db.queryRecords("MSN").getDiscountedCashFlowDto());
    }

    @Override
    public void insertFullRecord(final RecordHolder recordHolder) {
        //we can run these asynchronously using the ForkJoinPool as the backing Hikari pool has 5 db connections waiting to be used
        LOG.info("Starting parallel execution of database writes...");
        final long start = System.nanoTime();
        CompletableFuture<Void> c1 = CompletableFuture.runAsync(()->this.writeDiscountedCashFlowTable(recordHolder.getDiscountedCashFlowDto()));
        CompletableFuture<Void> c2 = CompletableFuture.runAsync(()->this.writePriceTargetSummaryTable(recordHolder.getPriceTargetSummaryDto()));
        CompletableFuture<Void> c3 = CompletableFuture.runAsync(()->this.writePriceTargetConsensusTable(recordHolder.getPriceTargetConsensusDto()));
        CompletableFuture<Void> c4 = CompletableFuture.allOf(c1, c2, c3);
        this.awaitAndHandleParallelExecution(c4, start);
    }

    private void awaitAndHandleParallelExecution(final CompletableFuture<Void> waiterThread, final long start){
        try {
            final long end = System.nanoTime();
            final long durationInMillis = Duration.ofNanos(end - start).toMillis();
            waiterThread.get();
            LOG.info("Parallel execution of database writes took {} milliseconds to run", durationInMillis);
        } catch (final InterruptedException interruptedException){
            LOG.error("The thread waiting for the parallel execution of database writes suddenly got interrupted!");
            Thread.currentThread().interrupt();
        } catch (final ExecutionException exception) {
            LOG.error("The following exception happened when writing parallel to database tables: ", exception);
        }
    }

    //todo create some objectToQueryMapper to reuse code
    private void writeDiscountedCashFlowTable(final DiscountedCashFlowDTO dto){
        if (dto == null){
            LOG.warn("Cannot write to discounted cashflow table as the provided data is null!");
            return;
        }
        try (final Connection conn = this.dataSource.getConnection(); final PreparedStatement preparedStatement = conn.prepareStatement(INSERT_INTO_DCF)){
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

    private void writePriceTargetSummaryTable(final PriceTargetSummaryDTO dto){
        if (dto == null){
            LOG.warn("Cannot write to target summary table as the provided data is null!");
            return;
        }
        try (final Connection conn = this.dataSource.getConnection(); final PreparedStatement preparedStatement = conn.prepareStatement(INSERT_INTO_PTS)){
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

    private void writePriceTargetConsensusTable(final PriceTargetConsensusDTO dto){
        if (dto == null){
            LOG.warn("Cannot write to target consensus table as the provided data is null!");
            return;
        }
        try (final Connection conn = this.dataSource.getConnection(); final PreparedStatement preparedStatement = conn.prepareStatement(INSERT_INTO_PTC)){
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
