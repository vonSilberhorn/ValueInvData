package com.szilberhornz.valueinvdata.services.stockvaluation.persistence;

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

/**
 * When querying for data, I don't attempt retries because the execution flow can just move on to hit the
 * next source of data (the FMP api). Query failures most likely only happen due to network or database availability issues,
 * and both of those have a great chance to prevent the success of an immediate retry.
 */
public final class ValuationDBRepositoryImpl implements ValuationDBRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ValuationDBRepositoryImpl.class);

    private final DataSource dataSource;

    public ValuationDBRepositoryImpl(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public RecordHolder queryRecords(final String ticker) {
        try(final Connection conn = this.dataSource.getConnection();
            final PreparedStatement preparedStatement = StatementDecorator.prepareQueryForAllRecordsOnTicker(conn, ticker)){
            final ResultSet resultSet = this.queryForTicker(preparedStatement, ticker, "database");
            return RecordMapper.newRecord(resultSet);
        } catch (final SQLException sqlException) {
            LOG.error("SQL execution to query for ticker {} failed due to the following reason:", ticker, sqlException);
            return null;
        }
    }

    @Override
    public DiscountedCashFlowDTO queryDiscountedCashFlowData(final String ticker) {
        try (final Connection conn = this.dataSource.getConnection();
        final PreparedStatement preparedStatement = StatementDecorator.prepareQueryForDiscountedCashFlowData(conn, ticker)){
            final ResultSet resultSet = this.queryForTicker(preparedStatement, ticker, "DiscountedCashFlowDb");
            return RecordMapper.newDcfDto(resultSet);
        } catch (final SQLException sqlException) {
            LOG.error("SQL execution to query DiscountedCashFlowDb for ticker {} failed due to the following reason:", ticker, sqlException);
            return null;
        }
    }

    @Override
    public PriceTargetSummaryDTO queryPriceTargetSummaryData(final String ticker) {
        try (final Connection conn = this.dataSource.getConnection();
             final PreparedStatement preparedStatement = StatementDecorator.prepareQueryForPriceTargetSummaryData(conn, ticker)){
            final ResultSet resultSet = this.queryForTicker(preparedStatement, ticker, "PriceTargetSummaryDb");
            return RecordMapper.newPtsDto(resultSet);
        } catch (final SQLException sqlException) {
            LOG.error("SQL execution to query PriceTargetSummaryDb for ticker {} failed due to the following reason:", ticker, sqlException);
            return null;
        }
    }

    @Override
    public PriceTargetConsensusDTO queryPriceTargetConsensusData(final String ticker) {
        try (final Connection conn = this.dataSource.getConnection();
             final PreparedStatement preparedStatement = StatementDecorator.prepareQueryForPriceTargetConsensusData(conn, ticker)){
            final ResultSet resultSet = this.queryForTicker(preparedStatement, ticker, "PriceTargetConsensusDb");
            return RecordMapper.newPtcDto(resultSet);
        } catch (final SQLException sqlException) {
            LOG.error("SQL execution to query PriceTargetConsensusDb for ticker {} failed due to the following reason:", ticker, sqlException);
            return null;
        }
    }

    @Override
    public void insertFullRecord(final RecordHolder recordHolder) {
        //we can run these asynchronously using the ForkJoinPool as the backing Hikari pool has 5 db connections waiting to be used
        LOG.info("Starting parallel execution of database writes...");
        final long start = System.nanoTime();
        final CompletableFuture<Void> c1 = CompletableFuture.runAsync(()->this.writeDiscountedCashFlowTable(recordHolder.getDiscountedCashFlowDto()));
        final CompletableFuture<Void> c2 = CompletableFuture.runAsync(()->this.writePriceTargetSummaryTable(recordHolder.getPriceTargetSummaryDto()));
        final CompletableFuture<Void> c3 = CompletableFuture.runAsync(()->this.writePriceTargetConsensusTable(recordHolder.getPriceTargetConsensusDto()));
        final CompletableFuture<Void> c4 = CompletableFuture.allOf(c1, c2, c3);
        this.awaitAndHandleParallelExecution(c4, start);
    }

    private ResultSet queryForTicker(final PreparedStatement preparedStatement, final String ticker, final String logMsg) throws SQLException {
        LOG.info("Starting to query {} for the records on ticker {}...", logMsg, ticker);
        final long start = System.nanoTime();
        final ResultSet resultSet = preparedStatement.executeQuery();
        final long end = System.nanoTime();
        final long durationInMillis = Duration.ofNanos(end - start).toMillis();
        LOG.info("Querying the {} for the records on ticker {} took {} milliseconds", logMsg, ticker, durationInMillis);
        return resultSet;
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

    private void writeDiscountedCashFlowTable(final DiscountedCashFlowDTO dto){
        if (dto == null){
            LOG.warn("Cannot write to discounted cashflow table as the provided data is null!");
            return;
        }
        try (final Connection conn = this.dataSource.getConnection();
             final PreparedStatement preparedStatement = StatementDecorator.prepareDiscountedCashFlowInsert(conn, dto)){
            final String logMsg = "discounted cashflow";
            this.insertRow(preparedStatement, dto.ticker(), logMsg);
        } catch (final SQLException sqlException) {
            LOG.error("Persisting data to the DiscountedCashFlowDb failed due to the following reason: ", sqlException);
        }
    }

    private void writePriceTargetSummaryTable(final PriceTargetSummaryDTO dto){
        if (dto == null){
            LOG.warn("Cannot write to target summary table as the provided data is null!");
            return;
        }
        try (final Connection conn = this.dataSource.getConnection();
             final PreparedStatement preparedStatement = StatementDecorator.preparePriceTargetSummaryInsert(conn, dto)){
            final String logMsg = "price target summary";
            this.insertRow(preparedStatement, dto.ticker(), logMsg);
        } catch (final SQLException sqlException) {
            LOG.error("Persisting data to the PriceTargetSummaryDb failed due to the following reason: ", sqlException);
        }
    }

    private void writePriceTargetConsensusTable(final PriceTargetConsensusDTO dto){
        if (dto == null){
            LOG.warn("Cannot write to target consensus table as the provided data is null!");
            return;
        }
        try (final Connection conn = this.dataSource.getConnection();
             final PreparedStatement preparedStatement = StatementDecorator.preparePriceTargetConsensusInsert(conn, dto)){
            final String logMsg = "price target consensus";
            this.insertRow(preparedStatement, dto.ticker(), logMsg);
        } catch (final SQLException sqlException) {
            LOG.error("Persisting data to the PriceTargetConsensusDb failed due to the following reason: ", sqlException);
        }
    }

    private void insertRow(final PreparedStatement preparedStatement, final String ticker, final String logMsg) throws SQLException {
        LOG.info("Writing {} data on ticker {} to the database...", logMsg, ticker);
        final long start = System.nanoTime();
        preparedStatement.executeUpdate();
        final long end = System.nanoTime();
        final long durationInMillis = Duration.ofNanos(end - start).toMillis();
        LOG.info("Writing {} data on ticker {} took {} milliseconds", logMsg, ticker, durationInMillis);
    }
}
