package com.chinthakad.statemachine.bloomfilter;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * PostgreSQL implementation of BackendStore using Hibernate ORM.
 * This provides persistent storage for the bloom filter backend.
 */
@ApplicationScoped
public class PostgreSQLBackendStore implements BackendStore {
    
    @Inject
    BloomFilterItemRepository repository;
    
    @Override
    public CompletableFuture<Boolean> contains(String item) {
        return CompletableFuture.supplyAsync((Supplier<Boolean>) () -> {
            return repository.find("item", item).firstResult() != null;
        });
    }
    
    @Override
    public CompletableFuture<Void> store(String item) {
        return CompletableFuture.runAsync(() -> {
            repository.persist(new BloomFilterItem(item));
        });
    }
    
    @Override
    public CompletableFuture<Long> getCount() {
        return CompletableFuture.supplyAsync(() -> {
            return repository.count();
        });
    }
    
    @Override
    public CompletableFuture<Void> clear() {
        return CompletableFuture.runAsync(() -> {
            repository.deleteAll();
        });
    }
    
    @Override
    public String getName() {
        return "PostgreSQL";
    }
    
    @Entity
    @Table(name = "bloom_filter_items")
    public static class BloomFilterItem {
        @Id
        private String item;
        
        public BloomFilterItem() {}
        
        public BloomFilterItem(String item) {
            this.item = item;
        }
        
        public String getItem() {
            return item;
        }
        
        public void setItem(String item) {
            this.item = item;
        }
    }
    
    @ApplicationScoped
    public static class BloomFilterItemRepository implements PanacheRepository<BloomFilterItem> {
    }
}
