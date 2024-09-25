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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * When querying for data, I don't attempt retries because the execution flow can just move on to hit the
 * next source of data (the FMP api). Query failures most likely only happen due to network or database availability issues,
 * and both of those have a great chance to prevent the success of an immediate retry.
 * <p>
 * For insertion, I implemented a very simple retry logic: if the CompletableFuture responsible to execute the
 * async insertion returns with an {@link AsyncRetryableException}, we run the original method again once, asynchronously,
 * as controlled in the {@link ValuationDBRepositoryImpl#retryExceptionallyAsync(Supplier)} method.
 * {@link AsyncRetryableException} is only thrown when the insert execution throws a retryable SQL Exception.
 * Retryability is controlled in the {@link FailureHandler} inner class.
 */
public final class ValuationDBRepositoryImpl implements ValuationDBRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ValuationDBRepositoryImpl.class);

    private final DataSource dataSource;

    private final FailureHandler failureHandler = new FailureHandler();

    public ValuationDBRepositoryImpl(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public RecordHolder queryRecords(final String ticker) {
        try (final Connection conn = this.dataSource.getConnection();
             final PreparedStatement preparedStatement = QueryMapper.prepareQueryForAllRecordsOnTicker(conn, ticker)) {
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
             final PreparedStatement preparedStatement = QueryMapper.prepareQueryForDiscountedCashFlowData(conn, ticker)) {
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
             final PreparedStatement preparedStatement = QueryMapper.prepareQueryForPriceTargetSummaryData(conn, ticker)) {
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
             final PreparedStatement preparedStatement = QueryMapper.prepareQueryForPriceTargetConsensusData(conn, ticker)) {
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
        final CompletableFuture<Void> c1 = this.retryExceptionallyAsync(() -> {
            this.writeDiscountedCashFlowTable(recordHolder.getDiscountedCashFlowDto());
            return null;
        });
        final CompletableFuture<Void> c2 = this.retryExceptionallyAsync(() -> {
            this.writePriceTargetSummaryTable(recordHolder.getPriceTargetSummaryDto());
            return null;
        });
        final CompletableFuture<Void> c3 = this.retryExceptionallyAsync(() -> {
            this.writePriceTargetConsensusTable(recordHolder.getPriceTargetConsensusDto());
            return null;
        });
        final CompletableFuture<Void> c4 = CompletableFuture.allOf(c1, c2, c3);
        this.awaitAndHandleParallelExecution(c4, start);
    }

    private <T> CompletableFuture<T> retryExceptionallyAsync(final Supplier<T> supplier) {
        //first run the job
        CompletableFuture<T> cf = CompletableFuture.supplyAsync(supplier);
        //rerun again in case of an AsyncRetryableException
        cf = cf.exceptionallyAsync(t -> {
            if (t.getCause() instanceof AsyncRetryableException) {
                supplier.get();
            }
            return null;
        });
        return cf;
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


    private void awaitAndHandleParallelExecution(final CompletableFuture<Void> waiterThread, final long start) {
        try {
            final long end = System.nanoTime();
            final long durationInMillis = Duration.ofNanos(end - start).toMillis();
            waiterThread.get();
            LOG.info("Parallel execution of database writes took {} milliseconds to run", durationInMillis);
        } catch (final InterruptedException interruptedException) {
            LOG.error("The thread waiting for the parallel execution of database writes suddenly got interrupted!");
            Thread.currentThread().interrupt();
        } catch (final ExecutionException exception) {
            LOG.error("The following exception happened when writing parallel to database tables: ", exception);
        }
    }

    private void writeDiscountedCashFlowTable(final DiscountedCashFlowDTO dto) {
        if (dto != null) {
            try (final Connection conn = this.dataSource.getConnection();
                 final PreparedStatement preparedStatement = QueryMapper.prepareDiscountedCashFlowInsert(conn, dto)) {
                final String logMsg = "discounted cashflow";
                this.insertRow(preparedStatement, dto.ticker(), logMsg);
            } catch (final SQLException sqlException) {
                final String logMsg = "DiscountedCashFlowDb";
                this.failureHandler.handleFailure(sqlException, logMsg);
            }
        } else {
            LOG.warn("Cannot write to discounted cashflow table as the provided data is null!");
        }
    }

    private void writePriceTargetSummaryTable(final PriceTargetSummaryDTO dto) {
        if (dto != null) {
            try (final Connection conn = this.dataSource.getConnection();
                 final PreparedStatement preparedStatement = QueryMapper.preparePriceTargetSummaryInsert(conn, dto)) {
                final String logMsg = "price target summary";
                this.insertRow(preparedStatement, dto.ticker(), logMsg);
            } catch (final SQLException sqlException) {
                final String logMsg = "PriceTargetSummaryDb";
                this.failureHandler.handleFailure(sqlException, logMsg);
            }
        } else {
            LOG.warn("Cannot write to target summary table as the provided data is null!");
        }
    }

    private void writePriceTargetConsensusTable(final PriceTargetConsensusDTO dto) {
        if (dto != null) {
            try (final Connection conn = this.dataSource.getConnection();
                 final PreparedStatement preparedStatement = QueryMapper.preparePriceTargetConsensusInsert(conn, dto)) {
                final String logMsg = "price target consensus";
                this.insertRow(preparedStatement, dto.ticker(), logMsg);
            } catch (final SQLException sqlException) {
                final String logMsg = "PriceTargetConsensusDb";
                this.failureHandler.handleFailure(sqlException, logMsg);
            }
        } else {
            LOG.warn("Cannot write to target consensus table as the provided data is null!");
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

    private static class FailureHandler {

        private static final Set<Integer> RETRYABLE_SQL_ERRORS = Set.of(
                617, //cannot locate work table, please run the query again
                1204, //cannot obtain lock
                1205, //deadlock
                1222, //lock request timeout
                1807, //cannot obtain exclusive lock
                3960, //snapshot update conflict
                8645 //timeout while waiting for memory
        );

        private void handleFailure(final SQLException sqlException, final String logMsg){
            if (this.isRetryable(sqlException)) {
                LOG.error("Persisting data to the {} failed due to the following reason, will try again ", logMsg, sqlException);
                throw new AsyncRetryableException(sqlException);
            }
            LOG.error("Persisting data to the {} failed due to the following, non-retryable reason: ", logMsg, sqlException);
        }

        private boolean isRetryable(final SQLException sqlException) {
            if (RETRYABLE_SQL_ERRORS.contains(sqlException.getErrorCode())) {
                return true;
            }
            if (sqlException.getCause() != null && sqlException.getCause() instanceof SQLException) {
                return this.isRetryable((SQLException) sqlException.getCause());
            }
            return false;
        }
    }
}
