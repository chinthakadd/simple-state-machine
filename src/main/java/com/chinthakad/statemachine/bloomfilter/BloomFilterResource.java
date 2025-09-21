package com.chinthakad.statemachine.bloomfilter;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * REST API endpoint for bloom filter duplicate detection
 */
@Path("/bloom-filter")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BloomFilterResource {
    
    private static final Logger LOG = Logger.getLogger(BloomFilterResource.class);
    
    @Inject
    BloomFilterService bloomFilterService;
    
    /**
     * Check if a UUID is a duplicate
     * @param request The request containing the UUID to check
     * @return Response with duplicate check result
     */
    @POST
    @Path("/check")
    public Response checkDuplicate(DuplicateCheckRequest request) {
        try {
            String item = request.getItem();
            if (item == null || item.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Item cannot be null or empty"))
                    .build();
            }
            
            CompletableFuture<BloomFilterService.DuplicateCheckResult> future = 
                bloomFilterService.checkAndStore(item);
            
            BloomFilterService.DuplicateCheckResult result = future.get();
            
            Map<String, Object> response = Map.of(
                "item", item,
                "isDuplicate", result.isDuplicate(),
                "wasFalsePositive", result.wasFalsePositive(),
                "message", result.getMessage()
            );
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error checking duplicate for request: %s", request);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Internal server error: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Generate a random UUID and check if it's a duplicate
     * @return Response with generated UUID and duplicate check result
     */
    @POST
    @Path("/check-random")
    public Response checkRandomDuplicate() {
        try {
            String randomUuid = UUID.randomUUID().toString();
            
            CompletableFuture<BloomFilterService.DuplicateCheckResult> future = 
                bloomFilterService.checkAndStore(randomUuid);
            
            BloomFilterService.DuplicateCheckResult result = future.get();
            
            Map<String, Object> response = Map.of(
                "item", randomUuid,
                "isDuplicate", result.isDuplicate(),
                "wasFalsePositive", result.wasFalsePositive(),
                "message", result.getMessage()
            );
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error checking random duplicate");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Internal server error: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Get bloom filter statistics
     * @return Response with bloom filter stats
     */
    @GET
    @Path("/stats")
    public Response getStats() {
        try {
            CompletableFuture<BloomFilterService.BloomFilterStats> future = 
                bloomFilterService.getStats();
            
            BloomFilterService.BloomFilterStats stats = future.get();
            
            Map<String, Object> response = Map.of(
                "expectedInsertions", stats.getExpectedInsertions(),
                "falsePositiveRate", stats.getFalsePositiveRate(),
                "backendCount", stats.getBackendCount(),
                "approximateElementCount", stats.getApproximateElementCount(),
                "backendStoreName", stats.getBackendStoreName()
            );
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error getting bloom filter stats");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Internal server error: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Clear all bloom filter data
     * @return Response indicating success
     */
    @DELETE
    @Path("/clear")
    public Response clear() {
        try {
            CompletableFuture<Void> future = bloomFilterService.clear();
            future.get();
            
            return Response.ok(Map.of("message", "Bloom filter cleared successfully")).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error clearing bloom filter");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Internal server error: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Request model for duplicate check
     */
    public static class DuplicateCheckRequest {
        private String item;
        
        public DuplicateCheckRequest() {}
        
        public DuplicateCheckRequest(String item) {
            this.item = item;
        }
        
        public String getItem() {
            return item;
        }
        
        public void setItem(String item) {
            this.item = item;
        }
    }
}

