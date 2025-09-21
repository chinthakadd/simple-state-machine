package com.chinthakad.statemachine.bloomfilter;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of BackendStore using ConcurrentHashMap.
 * This is suitable for development and testing, but not for production persistence.
 */
@ApplicationScoped
public class InMemoryBackendStore implements BackendStore {
    
    private final ConcurrentHashMap<String, Boolean> store = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(0);
    
    @Override
    public CompletableFuture<Boolean> contains(String item) {
        return CompletableFuture.completedFuture(store.containsKey(item));
    }
    
    @Override
    public CompletableFuture<Void> store(String item) {
        return CompletableFuture.runAsync(() -> {
            if (store.putIfAbsent(item, true) == null) {
                counter.incrementAndGet();
            }
        });
    }
    
    @Override
    public CompletableFuture<Long> getCount() {
        return CompletableFuture.completedFuture(counter.get());
    }
    
    @Override
    public CompletableFuture<Void> clear() {
        return CompletableFuture.runAsync(() -> {
            store.clear();
            counter.set(0);
        });
    }
    
    @Override
    public String getName() {
        return "InMemory";
    }
}

