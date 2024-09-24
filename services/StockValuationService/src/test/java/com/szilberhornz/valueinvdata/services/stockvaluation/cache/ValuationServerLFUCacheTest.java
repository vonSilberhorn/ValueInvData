package com.szilberhornz.valueinvdata.services.stockvaluation.cache;

import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ValuationServerLFUCacheTest {

    @Test
    void testEvictionWithOneExcessTickerInCache() {
        //rebalance threshold is high as we don't want to trigger it this way - it would run on a different,
        //non-blocking thread and test would not wait for it to complete. Instead, we trigger it manually later
        final ValuationServerLFUCache cache = new ValuationServerLFUCache(100, 2);
        final DiscountedCashFlowDTO appleDcfDto = new DiscountedCashFlowDTO("AAPL", "2024-09-24", 189, 220.2);
        final DiscountedCashFlowDTO microsoftDcfDto = new DiscountedCashFlowDTO("MSFT", "2024-09-24", 423, 433.88);
        final DiscountedCashFlowDTO amazonDcfDto = new DiscountedCashFlowDTO("AMZN", "2024-09-24", 154, 194.08);
        cache.put(appleDcfDto.ticker(), appleDcfDto);
        cache.put(microsoftDcfDto.ticker(), microsoftDcfDto);
        cache.put(amazonDcfDto.ticker(), amazonDcfDto);
        //get 2 tickers 10 times
        for (int i = 0; i < 10; i++) {
            cache.get(appleDcfDto.ticker());
            cache.get(microsoftDcfDto.ticker());
        }
        //get Apple one more time
        cache.get(appleDcfDto.ticker());
        //get 1 ticker 1 time
        cache.get(amazonDcfDto.ticker());
        //evictor should remove Amazon from the cache
        final ValuationServerLFUCache.LFUEvictor sut = cache.new LFUEvictor(cache);
        //calling this directly means that eviction runs on the main thread, so the test doesn't exit before the eviction ends
        sut.runEviction();
        assertEquals(appleDcfDto, cache.get(appleDcfDto.ticker()).getDiscountedCashFlowDto());
        assertEquals(microsoftDcfDto, cache.get(microsoftDcfDto.ticker()).getDiscountedCashFlowDto());
        assertNull(cache.get(amazonDcfDto.ticker()));
    }

    //in this test no eviction will happen because even though the capacity is 2 and there are 3 tickers in the cache,
    //the TreeMap will only return 2 as the size (since its key is the frequency). But due to the fact that
    //the eviction does not have to be too strict, it is an accepted behavior
    @Test
    void testEvictionWithOneExcessTickerButMatchingFrequencyCounts() {
        final ValuationServerLFUCache cache = new ValuationServerLFUCache(100, 2);
        final DiscountedCashFlowDTO appleDcfDto = new DiscountedCashFlowDTO("AAPL", "2024-09-24", 189, 220.2);
        final DiscountedCashFlowDTO microsoftDcfDto = new DiscountedCashFlowDTO("MSFT", "2024-09-24", 423, 433.88);
        final DiscountedCashFlowDTO amazonDcfDto = new DiscountedCashFlowDTO("AMZN", "2024-09-24", 154, 194.08);
        cache.put(appleDcfDto.ticker(), appleDcfDto);
        cache.put(microsoftDcfDto.ticker(), microsoftDcfDto);
        cache.put(amazonDcfDto.ticker(), amazonDcfDto);
        //get 2 tickers 10 times
        for (int i = 0; i < 10; i++) {
            cache.get(appleDcfDto.ticker());
            cache.get(microsoftDcfDto.ticker());
        }
        //get 1 ticker 1 time
        cache.get(amazonDcfDto.ticker());
        final ValuationServerLFUCache.LFUEvictor sut = cache.new LFUEvictor(cache);
        //calling this directly means that eviction runs on the main thread, so the test doesn't exit before the eviction ends
        sut.runEviction();
        //all three tickers will be present
        assertEquals(appleDcfDto, cache.get(appleDcfDto.ticker()).getDiscountedCashFlowDto());
        assertEquals(microsoftDcfDto, cache.get(microsoftDcfDto.ticker()).getDiscountedCashFlowDto());
        assertEquals(amazonDcfDto, cache.get(amazonDcfDto.ticker()).getDiscountedCashFlowDto());
    }

    @Test
    void noEvictionIfThresholdIsNotReached(){
        final ValuationServerLFUCache cache = new ValuationServerLFUCache(100, 2);
        final DiscountedCashFlowDTO appleDcfDto = new DiscountedCashFlowDTO("AAPL", "2024-09-24", 189, 220.2);
        final DiscountedCashFlowDTO microsoftDcfDto = new DiscountedCashFlowDTO("MSFT", "2024-09-24", 423, 433.88);
        final DiscountedCashFlowDTO amazonDcfDto = new DiscountedCashFlowDTO("AMZN", "2024-09-24", 154, 194.08);
        cache.put(appleDcfDto.ticker(), appleDcfDto);
        cache.put(microsoftDcfDto.ticker(), microsoftDcfDto);
        cache.put(amazonDcfDto.ticker(), amazonDcfDto);
        //get 2 tickers 43 times
        for (int i = 0; i < 44; i++) {
            cache.get(appleDcfDto.ticker());
            cache.get(microsoftDcfDto.ticker());
        }
        //get Apple one more so AAPL and MSFT don't have the same call count
        cache.get(appleDcfDto.ticker());
        //get AMZN ticker 1 time
        cache.get(amazonDcfDto.ticker());
        //all three tickers will be present as the evictor didn't run
        assertEquals(appleDcfDto, cache.get(appleDcfDto.ticker()).getDiscountedCashFlowDto());
        assertEquals(microsoftDcfDto, cache.get(microsoftDcfDto.ticker()).getDiscountedCashFlowDto());
        assertEquals(amazonDcfDto, cache.get(amazonDcfDto.ticker()).getDiscountedCashFlowDto());
    }
}
