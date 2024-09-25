package com.szilberhornz.valueinvdata.services.stockvaluation.persistence.inmem;

import com.szilberhornz.valueinvdata.services.stockvaluation.persistence.api.DataSourceFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.time.Duration;


/**
 * The in memory implementation initializes a datasource and executes a bunch of sql statements to
 * create a desired state: creating the tables and loading a bit of data. This is a mission-critical part
 * of the application (unless of course it doesn't use in-memory db) so any failure should mean the
 * termination of the app.
 */
public final class InMemoryDBDataSourceFactory implements DataSourceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDBDataSourceFactory.class);

    private final String initializerResourcePath;

    private final DataSource dataSource;

    //only one instance of this should exist, instantiated in the AppContainer class
    public InMemoryDBDataSourceFactory(final String initializerResourcePath) {
        this.initializerResourcePath = initializerResourcePath;
        this.dataSource = this.getHikariWrappedH2Instance();
        this.initializeInMemoryH2Db(this.dataSource);
    }

    @Override
    public DataSource getValuationDbDataSource() {
        return this.dataSource;
    }

    private void initializeInMemoryH2Db(final DataSource dataSource) {
        LOG.info("Starting in-memory H2DB initialization!");
        final long start = System.nanoTime();
        try (final Connection connection = dataSource.getConnection();
             final Statement statement = connection.createStatement()) {
            this.executeInitializerSqlStatements(statement);
            final long end = System.nanoTime();
            final long durationInMillis = Duration.ofNanos(end - start).toMillis();
            LOG.info("In-memory H2DB initialization finished in {} milliseconds!", durationInMillis);
        } catch (final SQLException | IOException exception) {
            throw new InMemoryDBInitializationFailedException("In-memory DB initialization failed!", exception);
        }
    }

    //this currentThread() should always be the [main] since initialization happens in the AppContainer, before
    //the app starts the http server
    private void executeInitializerSqlStatements(final Statement statement) throws SQLException, IOException {
        final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(this.initializerResourcePath);
        if (in != null) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            int sqlStatementCount = 0;
            while (reader.ready()) {
                sqlStatementCount++;
                final String sql = reader.readLine();
                LOG.info("Executing initializer statement: {}", sql);
                statement.addBatch(sql);
            }
            if (sqlStatementCount == 0) {
                throw new InMemoryDBInitializationFailedException("In-memory DB initialization failed because the " +
                        "initializer file didn't have any sql statements in it!\n" +
                        "At least the 'create table' statements must be present for successful startup!");
            } else {
                statement.executeBatch();
                LOG.info("Finished the execution of {} initializer sql statements!", sqlStatementCount);
            }
        } else {
            throw new InMemoryDBInitializationFailedException("In-memory DB initializer file doesn't exist!");
        }
    }

    private DataSource getHikariWrappedH2Instance() {
        final org.h2.jdbcx.JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:ValuationDB;MODE=MSSQLServer;DB_CLOSE_DELAY=-1");
        final HikariConfig hikariConfig = this.createInMemConfigForHikari();
        hikariConfig.setDataSource(h2DataSource);
        return new HikariDataSource(hikariConfig);
    }

    private HikariConfig createInMemConfigForHikari() {
        final HikariConfig hikariConfig = new HikariConfig();
        //fixed pool of 5 connections, probably way more than enough already,
        // but smaller than the Hikari default 10
        hikariConfig.setMaximumPoolSize(5);
        hikariConfig.setMinimumIdle(5);
        hikariConfig.setPoolName("InMemoryH2DBHikariPool");
        //idle connection health checks every 2 minutes
        final long twoMinutesInMillis = (long) 2 * 60 * 1000;
        hikariConfig.setKeepaliveTime(twoMinutesInMillis);
        return hikariConfig;
    }
}
