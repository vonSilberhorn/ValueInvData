package com.szilberhornz.valueinvdata.services.stockvaluation.cache;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * This class is a variation of Least Frequently Used Cache implementation. I chose the LFU method because I assume (!)
 * that in a real-world setting the queried tickers would follow a kind of Pareto distribution, which means that 80% of the
 * requests are coming for the top 20% of the tickers. After all, most people will look up valuations for S&P500 stocks.
 * <p>
 * In the default case of an LFU implementation, eviction happens in the put() method when the capacity is reached and
 * every get() method rebalances the frequencyCounter TreeMap, which is O(logN) time. I wanted to retain the logic of the
 * LFU while keeping the get() method at O(1). To achieve it, I chose to do the TreeMap manipulation only periodically
 * and on a non-blocking asynchronous thread. This means that the rebalance and eviction takes more time, but in fact that
 * is not a concern at all - the cache growing a bit over the expected capacity doesn't cause any issues unless the expected
 * capacity already occupies too much memory, which should never be the default case.
 * <p>
 * For the base LFU case I used this as a source: <a href="https://www.geeksforgeeks.org/implement-a-cache-eviction-policy-using-treemap-in-java/">LFU</a>
 */
public class ValuationServerLFUCache extends ValuationServerCache {

    private final int capacity;
    private final int rebalanceThreshold;

    //this will tell us when we need to trigger async eviction. This could very well be made thread safe by using the
    //AtomicInteger instead, but we can be very lax about the trigger, a few missed counter step won't matter at all
    private int counter = 0;

    private final CacheEvictor cacheEvictor = new LFUEvictor(this);

    //like the counter above, the frequency map and the frequencyCounter don't need to be thread safe as
    //data consistency in them doesn't matter that much
    private final Map<String, Integer> frequencyMap = new HashMap<>();
    private final TreeMap<Integer, LinkedHashSet<String>> frequencyCounter = new TreeMap<>();

    public ValuationServerLFUCache(final int rebalanceThreshold, final int capacity) {
        this.rebalanceThreshold = rebalanceThreshold;
        this.capacity = capacity;
    }

    /**
     *  The get method is still O(1) while in a classic LFU it would be O(logN)
     *  Every {@link ValuationServerLFUCache#rebalanceThreshold}-th call of this method starts a thread and runs the
     *  {@link CacheEvictor#runEviction()} method in a non-blocking way
     */
    @Override
    @Nullable
    public RecordHolder get (final String ticker) {
        if (!this.valuationServerCache.containsKey(ticker)){
            return null;
        }
        this.counter++;
        this.frequencyMap.compute(ticker, (k, v) -> v == null ? 1 : v + 1);
        if (this.counter >= this.rebalanceThreshold) {
            this.counter = 0;
            new CompletableFuture<>().completeAsync(()-> this.cacheEvictor);
        }
        return this.valuationServerCache.get(ticker);
    }

    class LFUEvictor implements CacheEvictor {

        private static final Logger LOG = LoggerFactory.getLogger(LFUEvictor.class);

        final ValuationServerLFUCache cache;

        LFUEvictor(final ValuationServerLFUCache cache) {
            this.cache = cache;
        }

        @Override
        public void runEviction() {
            final long start = System.nanoTime();
            LOG.info("Starting rebalance and cache eviction scan");
            this.rebalanceFrequencyCounter();
            this.evictExcess();
            final long end = System.nanoTime();
            final long durationInMillis = Duration.ofNanos(end - start).toMillis();
            LOG.info("Rebalance and cache eviction took {} milliseconds", durationInMillis);
        }

        private void rebalanceFrequencyCounter(){
            for (final Map.Entry<String, Integer> entry : this.cache.frequencyMap.entrySet()) {
                //update frequency
                final int frequency = this.cache.frequencyMap.get(entry.getKey());
                //update frequency counter
                this.cache.frequencyCounter.computeIfAbsent(frequency, k -> new LinkedHashSet<>()).add(entry.getKey());
            }
        }

        private void evictExcess() {
            final int evictCount = this.cache.frequencyCounter.size() - ValuationServerLFUCache.this.capacity;
            if (evictCount > 0) {
                LOG.info("Starting eviction of {} tickers", evictCount);
                for (int i = 0; i < evictCount; i++) {
                    final int lowestFrequency = this.cache.frequencyCounter.firstKey();
                    final String leastFrequentKey = this.cache.frequencyCounter.get(lowestFrequency).getFirst();

                    // Remove from cache and frequency maps
                    this.cache.valuationServerCache.remove(leastFrequentKey);
                    this.cache.frequencyMap.remove(leastFrequentKey);

                    // Remove from frequencyCounter
                    this.cache.frequencyCounter.get(lowestFrequency).remove(leastFrequentKey);
                    if (this.cache.frequencyCounter.get(lowestFrequency).isEmpty()) {
                        this.cache.frequencyCounter.remove(lowestFrequency);
                    }
                }
            } else {
                LOG.info("No cache eviction needed at this time");
            }
        }
    }
}
