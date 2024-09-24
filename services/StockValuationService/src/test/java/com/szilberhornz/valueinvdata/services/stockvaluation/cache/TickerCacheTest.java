package com.szilberhornz.valueinvdata.services.stockvaluation.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TickerCacheTest {

    @Test
    void testSuccessfulInitialization(){
        String testResourceFileName = "testTickers.txt";
        final TickerCache sut = new TickerCache(testResourceFileName);
        assertTrue(sut.tickerExists("GOGN"));
        assertTrue(sut.tickerExists("agorf"));
        assertFalse(sut.tickerExists("AAPL"));
    }

    @Test
    void testInitializationWithNonexistentFile(){
        String testResourceFileName = "nonexistentTickersFile.txt";
        final Exception exception = assertThrows(TickerCacheLoadingFailedException.class, () -> new TickerCache(testResourceFileName));
        assertEquals("Ticker cache loading failed because the resource file nonexistentTickersFile.txt does not exist or is empty!", exception.getMessage());
    }

    @Test
    void testEmptyInitialization(){
        String testResourceFileName = "emptyTickersFile.txt";
        final Exception exception = assertThrows(TickerCacheLoadingFailedException.class, () -> new TickerCache(testResourceFileName));
        assertEquals("Ticker cache loading failed because the resource file emptyTickersFile.txt does not exist or is empty!", exception.getMessage());
    }
}
