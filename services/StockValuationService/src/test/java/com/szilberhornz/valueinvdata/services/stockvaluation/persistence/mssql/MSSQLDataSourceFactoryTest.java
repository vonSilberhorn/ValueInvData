package com.szilberhornz.valueinvdata.services.stockvaluation.persistence.mssql;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

//we cannot really test the construction of the DataSource itself, as for that we would need to have an actual MSSQL
//instance somewhere, but we can test the configuration itself
class MSSQLDataSourceFactoryTest {

    @Test
    void initializationFailsWithoutUserName(){
        final Exception expected = assertThrows(IllegalStateException.class, MSSQLDataSourceFactory::new);
        assertEquals("The application cannot start without a user name for the MSSQL server!\n" +
                "Please specify the user name with the 'MSSQL_USER' VM Option and try again!", expected.getMessage());
    }

    @Test
    @SetSystemProperty(key = "MSSQL_USER", value = "sa")
    void initializationFailsWithoutPw(){
        final Exception expected = assertThrows(IllegalStateException.class, MSSQLDataSourceFactory::new);
        assertEquals("The application cannot start without a passwd for the MSSQL server!\n" +
        "Please specify the passwd with the 'MSSQL_PW' VM Option and try again!", expected.getMessage());
    }

    @Test
    @SetSystemProperty(key = "MSSQL_USER", value = "sa")
    @SetSystemProperty(key = "MSSQL_PW", value = "sa")
    void initializationFailsWithoutMSSQLAddress(){
        final Exception expected = assertThrows(IllegalStateException.class, MSSQLDataSourceFactory::new);
        assertEquals("The application cannot start without an address pointing to the MSSQL server!\n" +
                "Please specify the MSSQL address (e.g. servername\\\\instancename:portnumber) with the 'MSSQL_ADDRESS' VM Option and try again!", expected.getMessage());
    }

    @Test
    void assertPoolProperties(){
        final MSSQLDataSourceFactory factory = new MSSQLDataSourceFactory(null);
        final HikariConfig config = factory.createHikariConfigWithPoolProperties();
        assertEquals(10, config.getMaximumPoolSize());
        assertEquals(5, config.getMinimumIdle());
        assertEquals(120000, config.getKeepaliveTime());
        assertEquals("ValuationDbHikariPool", config.getPoolName());
    }

    @Test
    void assertConnectionProperties(){
        final MSSQLDataSourceFactory factory = new MSSQLDataSourceFactory(null);
        final Properties properties = factory.createDefaultConnectionProperties();
        assertEquals("false", properties.getProperty("disableStatementPooling"));
        assertEquals("true", properties.getProperty("enablePrepareOnFirstPreparedStatementCall"));
        assertEquals("10", properties.getProperty("statementPoolingCacheSize"));
    }
}
