package com.szilberhornz.valueinvdata.services.stockvaluation.persistence;

import com.szilberhornz.valueinvdata.services.stockvaluation.AppContext;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.sql.*;

public class ValuationServiceDataSourceFactory implements DataSourceFactory {

    @Override
    public DataSource getValuationDBDataSource() {
        final boolean useMsSQL = AppContext.USE_MSSQL;
        if (useMsSQL) {
            //todo
            return null;
        } else {
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

        private static final String CREATE_TABLE_VALUATION_DB = "CREATE TABLE valuation_db (Ticker varchar (255)," +
                "Dcf varchar (255)" +
                ")";
        private static final String INITIALIZE_DATA_VALUATION_DB = "INSERT INTO valuation_db VALUES ('AAPL', '168.2')";

        public static DataSource getH2DbInstance(){
            org.h2.jdbcx.JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:ValuationDB;MODE=MSSQLServer;DB_CLOSE_DELAY=-1");
            dataSource.setUser("sa");
            dataSource.setPassword("sa");
            initializeDataBase(dataSource);
            return dataSource;
        }

        static void initializeDataBase(final DataSource dataSource) {
            try (final Connection conn = dataSource.getConnection(); final Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(CREATE_TABLE_VALUATION_DB);
                stmt.executeUpdate(INITIALIZE_DATA_VALUATION_DB);
            } catch (SQLException sqlException) {
                System.out.println("Hot diggity damn!");
            }
        }
    }
}
