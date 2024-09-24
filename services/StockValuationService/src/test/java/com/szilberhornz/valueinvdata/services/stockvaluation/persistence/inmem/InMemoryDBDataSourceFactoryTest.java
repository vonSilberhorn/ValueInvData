package com.szilberhornz.valueinvdata.services.stockvaluation.persistence.inmem;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

//the h2 in-mem database is originally designed to run in tests scope, so there's no need to try and mock it here
class InMemoryDBDataSourceFactoryTest {

    @Test
    void testHikariConfig(){
        final InMemoryDBDataSourceFactory sut = new InMemoryDBDataSourceFactory("simpleDbInitializer.txt");
        final DataSource inMemDs = sut.getValuationDbDataSource();
        assertInstanceOf(HikariDataSource.class, inMemDs);
        final HikariDataSource hikariDs = (HikariDataSource) inMemDs;
        assertEquals(5, hikariDs.getMaximumPoolSize());
        assertEquals(5, hikariDs.getMinimumIdle());
        assertEquals("InMemoryH2DBHikariPool", hikariDs.getPoolName());
        assertEquals(120000, hikariDs.getKeepaliveTime());
    }

    @Test
    void inMemoryDsInitShouldFailWithNonExistentFile(){
        final Exception exception =
                assertThrows(InMemoryDBInitializationFailedException.class, ()-> new InMemoryDBDataSourceFactory("nonExistentFile"));
        assertEquals("In-memory DB initializer file doesn't exist!", exception.getMessage());
    }

    @Test
    void emptyFileShouldCauseFailure(){
        final Exception exception =
                assertThrows(InMemoryDBInitializationFailedException.class, ()-> new InMemoryDBDataSourceFactory("emptyDbInitializer.txt"));
        assertEquals("In-memory DB initialization failed because the initializer file didn't have any sql statements in it!\n" +
                "At least the 'create table' statements must be present for successful startup!", exception.getMessage());
    }

    @Test
    void exceptionDuringInitializationShouldTriggerFailure(){
        final Exception exception =
                assertThrows(InMemoryDBInitializationFailedException.class, ()-> new InMemoryDBDataSourceFactory("faultyDbInitializer.txt"));
        assertEquals("In-memory DB initialization failed!", exception.getMessage());
        assertTrue(exception.getCause().getMessage().startsWith("Table \"PRICETARGETSUMMARYDB\" not found"));
    }
}
