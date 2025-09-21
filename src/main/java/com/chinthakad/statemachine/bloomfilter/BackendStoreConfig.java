package com.chinthakad.statemachine.bloomfilter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Configuration class for selecting the appropriate backend store
 */
@ApplicationScoped
public class BackendStoreConfig {
    
    @ConfigProperty(name = "bloom.filter.backend.store", defaultValue = "in-memory")
    String backendStoreType;
    
    @Produces
    @ApplicationScoped
    public BackendStore backendStore(InMemoryBackendStore inMemoryStore, 
                                   PostgreSQLBackendStore postgresStore) {
        return switch (backendStoreType.toLowerCase()) {
            case "postgresql", "postgres" -> postgresStore;
            case "in-memory", "memory" -> inMemoryStore;
            default -> {
                System.out.println("Unknown backend store type: " + backendStoreType + 
                                 ". Defaulting to in-memory store.");
                yield inMemoryStore;
            }
        };
    }
}

