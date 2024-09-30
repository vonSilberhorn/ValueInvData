package com.szilberhornz.valueinvdata.services.stockvaluation.utility.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TickerCacheTest {

    @Test
    void testSuccessfulInitialization(){
        final String testResourceFileName = "testTickers.txt";
        final TickerCache sut = new TickerCache(testResourceFileName);
        assertTrue(sut.tickerExists("GOGN"));
        assertTrue(sut.tickerExists("agorf"));
        assertFalse(sut.tickerExists("AAPL"));
    }

    @Test
    void testInitializationWithNonexistentFile(){
        final String testResourceFileName = "nonexistentTickersFile.txt";
        final Exception exception = assertThrows(TickerCacheLoadingFailedException.class, () -> new TickerCache(testResourceFileName));
        assertEquals("Ticker cache loading failed because the resource file nonexistentTickersFile.txt does not exist or is empty!", exception.getMessage());
    }

    @Test
    void testEmptyInitialization(){
        final String testResourceFileName = "emptyTickersFile.txt";
        final Exception exception = assertThrows(TickerCacheLoadingFailedException.class, () -> new TickerCache(testResourceFileName));
        assertEquals("Ticker cache loading failed because the resource file emptyTickersFile.txt does not exist or is empty!", exception.getMessage());
    }
}
