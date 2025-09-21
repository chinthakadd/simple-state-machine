package com.chinthakad.statemachine.bloomfilter;

import java.util.concurrent.CompletableFuture;

/**
 * Pluggable backend store interface for bloom filter persistence.
 * This allows switching between different storage backends (in-memory, PostgreSQL, Redis, etc.)
 */
public interface BackendStore {
    
    /**
     * Check if an item exists in the backend store
     * @param item The item to check
     * @return CompletableFuture that completes with true if item exists, false otherwise
     */
    CompletableFuture<Boolean> contains(String item);
    
    /**
     * Store an item in the backend store
     * @param item The item to store
     * @return CompletableFuture that completes when the item is stored
     */
    CompletableFuture<Void> store(String item);
    
    /**
     * Get the total number of items stored
     * @return CompletableFuture that completes with the count
     */
    CompletableFuture<Long> getCount();
    
    /**
     * Clear all stored items
     * @return CompletableFuture that completes when cleared
     */
    CompletableFuture<Void> clear();
    
    /**
     * Get the name/type of this backend store
     * @return The backend store name
     */
    String getName();
}

