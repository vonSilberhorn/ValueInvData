package com.szilberhornz.valueinvdata.services.stockvaluation.persistence.inmem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryDBDataSourceFactoryTest {

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
}
