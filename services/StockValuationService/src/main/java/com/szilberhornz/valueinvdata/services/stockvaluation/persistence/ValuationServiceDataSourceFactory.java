package com.szilberhornz.valueinvdata.services.stockvaluation.persistence;

import com.szilberhornz.valueinvdata.services.stockvaluation.AppContext;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.sql.*;

//todo split it to InMem + MsSQL and put the initializer sql statements in a resource file. Turn on MSSQL statement cache
public class ValuationServiceDataSourceFactory implements DataSourceFactory {

    @Override
    public DataSource getValuationDBDataSource() {
        final boolean useMsSQL = AppContext.USE_MSSQL;
        if (useMsSQL) {
            //todo create the MSSQL one assuming there is a working instance at the address with all the tables setup
            return null;
        } else {
            //todo back this with HikariCP
            return this.createH2DBWithTSqlSyntax();
        }
    }

    private DataSource createH2DBWithTSqlSyntax() {
        return LazyInMemoryDBHolder.H2_DATASOURCE_INSTANCE;
    }


    //lazy initialize in-memory db, as the app might not actually need it
    private static class LazyInMemoryDBHolder {

        public static final DataSource H2_DATASOURCE_INSTANCE =H2DbInitializer.getH2DbInstance();

    }

    private static class H2DbInitializer {

        private static final String CREATE_TABLE_DCF =
                "CREATE TABLE DiscountedCashFlowDb (Ticker VARCHAR (255) PRIMARY KEY," +
                        "Dcf DECIMAL (12, 2)," +
                        "Date DATE," +
                        "StockPrice DECIMAL (12, 2)" +
                        ")";
        private static final String INITIALIZE_DCF = "INSERT INTO DiscountedCashFlowDb VALUES ('AAPL', 182.236, '2024-09-23', 228.2)";

        private static final String CREATE_TABLE_PRICE_TARGET_SUMMARY =
                "CREATE TABLE PriceTargetSummaryDb (" +
                        "Ticker VARCHAR (255) PRIMARY KEY," +
                        "LastMonth SMALLINT," +
                        "LastMonthAvgPriceTarget DECIMAL (12, 2)," +
                        "LastQuarter SMALLINT," +
                        "LastQuarterAvgPriceTarget DECIMAL (12, 2)" +
                        ")";
        private static final String INITIALIZE_PRICE_TARGET = "INSERT INTO PriceTargetSummaryDb VALUES " +
                "('AAPL', 5, 220.2, 11, 217.18)";

        private static final String CREATE_TABLE_PRICE_TARGET_CONSENSUS =
                "CREATE TABLE PriceTargetConsensusDb (" +
                        "Ticker VARCHAR (255) PRIMARY KEY," +
                        "TargetHigh DECIMAL (12, 2)," +
                        "TargetLow DECIMAL (12, 2)," +
                        "TargetConsensus DECIMAL (12, 2)," +
                        "TargetMedian DECIMAL (12, 2)" +
                        ")";
        private static final String INITIALIZE_PRICE_TARGET_CONSENSUS = "INSERT INTO PriceTargetConsensusDb VALUES " +
                "('AAPL', 240, 110, 189.18, 195)";

        public static DataSource getH2DbInstance(){
            org.h2.jdbcx.JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:ValuationDB;MODE=MSSQLServer;DB_CLOSE_DELAY=-1");
            dataSource.setUser("sa");
            dataSource.setPassword("sa");
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDataSource(dataSource);
            HikariDataSource hikariDs = new HikariDataSource(hikariConfig);
            initializeDataBase(hikariDs);
            return dataSource;
        }

        static void initializeDataBase(final DataSource dataSource) {
            try (final Connection conn = dataSource.getConnection(); final Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(CREATE_TABLE_DCF);
                stmt.executeUpdate(CREATE_TABLE_PRICE_TARGET_SUMMARY);
                stmt.executeUpdate(CREATE_TABLE_PRICE_TARGET_CONSENSUS);
                stmt.executeUpdate(INITIALIZE_DCF);
                stmt.executeUpdate(INITIALIZE_PRICE_TARGET);
                stmt.executeUpdate(INITIALIZE_PRICE_TARGET_CONSENSUS);
            } catch (SQLException sqlException) {
                System.out.println("Hot diggity damn!");
            }
        }
    }
}
