package com.szilberhornz.valueinvdata.services.stockvaluation.persistence.mssql;

import com.szilberhornz.valueinvdata.services.stockvaluation.AppContext;
import com.szilberhornz.valueinvdata.services.stockvaluation.persistence.api.DataSourceFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * This class is responsible for initializing a DataSource pointing to an up and running MSSQL server elsewhere.
 * If the MSSQL_ADDRESS VM Option is passed at startup, the application will invoke this class instead of the in-memory db
 * <p>
 * This DataSource is backed by a HikariCP connection pool.
 */
public class MSSQLDataSourceFactory implements DataSourceFactory {

    private final DataSource dataSource;

    @Override
    public DataSource getValuationDbDataSource() {
        return this.dataSource;
    }

    public MSSQLDataSourceFactory(){
        this.dataSource = this.constructHikariDataSource();
    }

    //ctor for testing only
    MSSQLDataSourceFactory(final DataSource dataSource){
        this.dataSource = dataSource;
    }

    private DataSource constructHikariDataSource() {
        final HikariConfig hikariConfig = this.createHikariConfigWithPoolProperties();
        //add the basics
        hikariConfig.setUsername(this.getUser());
        hikariConfig.setPassword(new String(this.getPw()));
        hikariConfig.setJdbcUrl(this.buildJdbcUrl());
        //add MSSQL connection properties
        hikariConfig.setDataSourceProperties(this.createDefaultConnectionProperties());
        return new HikariDataSource(hikariConfig);
    }

    String buildJdbcUrl(){
        if (AppContext.MSSQL_ADDRESS == null){
            throw new IllegalStateException("The application cannot start without an address pointing to the MSSQL server!\n" +
                    "Please specify the MSSQL address (e.g. servername\\\\instancename:portnumber) with the 'MSSQL_ADDRESS' VM Option and try again!");
        }
        return "jdbc:sqlserver://" + AppContext.MSSQL_ADDRESS;
    }

    Properties createDefaultConnectionProperties(){
        final Properties properties = new Properties();
        properties.setProperty("disableStatementPooling", "false");
        properties.setProperty("enablePrepareOnFirstPreparedStatementCall", "true");
        properties.setProperty("statementPoolingCacheSize", "10");
        return properties;
    }

    HikariConfig createHikariConfigWithPoolProperties(){
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(5);
        //default idle timeout is 10 minutes, which is ok for us.
        //It only culls the idle connection over the minimumIdle setting
        //idle connection health check every 2 minutes
        final long twoMinutesInMillis = (long) 2 * 60 * 1000;
        hikariConfig.setKeepaliveTime(twoMinutesInMillis);
        hikariConfig.setPoolName("ValuationDbHikariPool");
        return hikariConfig;
    }

    private String getUser(){
        final String user = System.getProperty("MSSQL_USER");
        if (user == null){
            throw new IllegalStateException("The application cannot start without a user name for the MSSQL server!\n" +
                    "Please specify the user name with the 'MSSQL_USER' VM Option and try again!");
        }
        return user;
    }

    private char[] getPw(){
        final String pw = System.getProperty("MSSQL_PW");
        if (pw == null){
            throw new IllegalStateException("The application cannot start without a passwd for the MSSQL server!\n" +
                    "Please specify the passwd with the 'MSSQL_PW' VM Option and try again!");
        }
        return pw.toCharArray();
    }
}
