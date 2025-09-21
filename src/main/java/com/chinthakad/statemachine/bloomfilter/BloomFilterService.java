package com.chinthakad.statemachine.bloomfilter;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Bloom Filter Service that combines:
 * 1. Google Guava Bloom Filter for fast duplicate detection
 * 2. In-memory cache for false positive verification
 * 3. Pluggable backend store for persistence
 */
@ApplicationScoped
public class BloomFilterService {
    
    private static final Logger LOG = Logger.getLogger(BloomFilterService.class);
    
    @Inject
    @CacheName("bloom-filter-cache")
    Cache cache;
    
    @Inject
    BackendStore backendStore;
    
    private BloomFilter<String> bloomFilter;
    
    @ConfigProperty(name = "bloom.filter.expected.insertions", defaultValue = "1000000")
    long expectedInsertions;
    
    @ConfigProperty(name = "bloom.filter.false.positive.rate", defaultValue = "0.01")
    double falsePositiveRate;
    
    public BloomFilterService() {
        // Initialize bloom filter with default values
        this.bloomFilter = BloomFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8),
            expectedInsertions,
            falsePositiveRate
        );
    }
    
    /**
     * Check if an item is a duplicate and store it if it's new
     * @param item The item to check
     * @return CompletableFuture with DuplicateCheckResult
     */
    public CompletableFuture<DuplicateCheckResult> checkAndStore(String item) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // First, check the bloom filter
                boolean mightContain = bloomFilter.mightContain(item);
                
                if (!mightContain) {
                    // Bloom filter says it's definitely new
                    bloomFilter.put(item);
                    backendStore.store(item).join();
                    cache.invalidate(item).await().indefinitely();
                    
                    LOG.debugf("Item %s is new (bloom filter)", item);
                    return new DuplicateCheckResult(false, false, "New item");
                } else {
                    // Bloom filter says it might be a duplicate - check cache first
                    Boolean cacheResult = cache.get(item, key -> {
                        // Cache miss - check backend store
                        return backendStore.contains(key).join();
                    }).await().indefinitely();
                    
                    if (cacheResult != null && cacheResult) {
                        // Confirmed duplicate in cache/backend
                        LOG.debugf("Item %s is confirmed duplicate (cache/backend)", item);
                        return new DuplicateCheckResult(true, false, "Confirmed duplicate");
                    } else {
                        // False positive - item is actually new
                        bloomFilter.put(item);
                        backendStore.store(item).join();
                        cache.invalidate(item).await().indefinitely();
                        
                        LOG.debugf("Item %s is new (false positive resolved)", item);
                        return new DuplicateCheckResult(false, true, "New item (false positive resolved)");
                    }
                }
            } catch (Exception e) {
                LOG.errorf(e, "Error checking duplicate for item: %s", item);
                return new DuplicateCheckResult(false, false, "Error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Get statistics about the bloom filter
     * @return CompletableFuture with BloomFilterStats
     */
    public CompletableFuture<BloomFilterStats> getStats() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long backendCount = backendStore.getCount().join();
                return new BloomFilterStats(
                    expectedInsertions,
                    falsePositiveRate,
                    backendCount,
                    bloomFilter.approximateElementCount(),
                    backendStore.getName()
                );
            } catch (Exception e) {
                LOG.errorf(e, "Error getting bloom filter stats");
                throw new RuntimeException("Failed to get stats", e);
            }
        });
    }
    
    /**
     * Clear all data (bloom filter, cache, and backend store)
     * @return CompletableFuture that completes when cleared
     */
    public CompletableFuture<Void> clear() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Create a new bloom filter
                this.bloomFilter = BloomFilter.create(
                    Funnels.stringFunnel(StandardCharsets.UTF_8),
                    expectedInsertions,
                    falsePositiveRate
                );
                
                // Clear backend store
                backendStore.clear().join();
                
                // Clear cache
                cache.invalidateAll().await().indefinitely();
                
                LOG.info("Bloom filter system cleared successfully");
            } catch (Exception e) {
                LOG.errorf(e, "Error clearing bloom filter system");
                throw new RuntimeException("Failed to clear system", e);
            }
        });
    }
    
    /**
     * Result of a duplicate check operation
     */
    public static class DuplicateCheckResult {
        private final boolean isDuplicate;
        private final boolean wasFalsePositive;
        private final String message;
        
        public DuplicateCheckResult(boolean isDuplicate, boolean wasFalsePositive, String message) {
            this.isDuplicate = isDuplicate;
            this.wasFalsePositive = wasFalsePositive;
            this.message = message;
        }
        
        public boolean isDuplicate() {
            return isDuplicate;
        }
        
        public boolean wasFalsePositive() {
            return wasFalsePositive;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * Statistics about the bloom filter system
     */
    public static class BloomFilterStats {
        private final long expectedInsertions;
        private final double falsePositiveRate;
        private final long backendCount;
        private final long approximateElementCount;
        private final String backendStoreName;
        
        public BloomFilterStats(long expectedInsertions, double falsePositiveRate, 
                              long backendCount, long approximateElementCount, String backendStoreName) {
            this.expectedInsertions = expectedInsertions;
            this.falsePositiveRate = falsePositiveRate;
            this.backendCount = backendCount;
            this.approximateElementCount = approximateElementCount;
            this.backendStoreName = backendStoreName;
        }
        
        public long getExpectedInsertions() {
            return expectedInsertions;
        }
        
        public double getFalsePositiveRate() {
            return falsePositiveRate;
        }
        
        public long getBackendCount() {
            return backendCount;
        }
        
        public long getApproximateElementCount() {
            return approximateElementCount;
        }
        
        public String getBackendStoreName() {
            return backendStoreName;
        }
    }
}

