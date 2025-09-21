# Bloom Filter REST API System

A high-performance, extensible bloom filter system for REST APIs that provides fast duplicate detection with configurable backend storage.

## Features

- **Fast Duplicate Detection**: Uses Google Guava's Bloom Filter for O(1) lookup performance
- **False Positive Handling**: In-memory cache to verify bloom filter false positives
- **Pluggable Backend Storage**: Support for in-memory and PostgreSQL storage
- **REST API**: Simple HTTP endpoints for duplicate checking
- **Statistics**: Real-time bloom filter statistics and monitoring
- **Configurable**: Adjustable bloom filter parameters and backend selection

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   REST API      │    │  Bloom Filter   │    │  In-Memory      │
│   Endpoint      │───▶│  (Guava)        │───▶│  Cache          │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │  Backend Store  │
                       │  (In-Memory/    │
                       │   PostgreSQL)   │
                       └─────────────────┘
```

## API Endpoints

### Check Duplicate
```http
POST /bloom-filter/check
Content-Type: application/json

{
  "item": "your-uuid-or-string-here"
}
```

**Response:**
```json
{
  "item": "your-uuid-or-string-here",
  "isDuplicate": false,
  "wasFalsePositive": false,
  "message": "New item"
}
```

### Check Random UUID
```http
POST /bloom-filter/check-random
```

**Response:**
```json
{
  "item": "generated-uuid",
  "isDuplicate": false,
  "wasFalsePositive": false,
  "message": "New item"
}
```

### Get Statistics
```http
GET /bloom-filter/stats
```

**Response:**
```json
{
  "expectedInsertions": 1000000,
  "falsePositiveRate": 0.01,
  "backendCount": 150,
  "approximateElementCount": 150,
  "backendStoreName": "InMemory"
}
```

### Clear All Data
```http
DELETE /bloom-filter/clear
```

**Response:**
```json
{
  "message": "Bloom filter cleared successfully"
}
```

## Configuration

### Application Properties

```properties
# Bloom Filter Configuration
bloom.filter.expected.insertions=1000000
bloom.filter.false.positive.rate=0.01
bloom.filter.backend.store=in-memory

# Cache Configuration
quarkus.cache.caffeine.bloom-filter-cache.maximum-size=10000
quarkus.cache.caffeine.bloom-filter-cache.expire-after-write=1h

# PostgreSQL Configuration (for future use)
# quarkus.datasource.db-kind=postgresql
# quarkus.datasource.username=postgres
# quarkus.datasource.password=postgres
# quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/bloomfilter
# quarkus.hibernate-orm.database.generation=drop-and-create
```

### Backend Store Options

- **in-memory**: Uses ConcurrentHashMap (default, suitable for development)
- **postgresql**: Uses PostgreSQL database (suitable for production)

## How It Works

1. **Bloom Filter Check**: First, the system checks if the item might exist in the bloom filter
2. **New Item**: If the bloom filter says the item is definitely new, it's added to both the bloom filter and backend store
3. **Potential Duplicate**: If the bloom filter says the item might exist, the system checks the in-memory cache
4. **Cache Hit**: If found in cache, it's confirmed as a duplicate
5. **Cache Miss**: If not in cache, the system checks the backend store
6. **False Positive**: If not in backend store, it's a false positive - the item is added to all stores

## Performance Characteristics

- **Bloom Filter**: O(1) lookup time, very fast
- **Cache**: O(1) lookup time, handles false positives
- **Backend Store**: Varies by implementation (O(1) for in-memory, O(log n) for database)

## False Positive Rate

The system is configured with a 1% false positive rate by default. This means:
- 99% of "not seen" responses are definitely correct
- 1% of "might be seen" responses require additional verification
- The cache and backend store handle false positive verification

## Usage Examples

### Using cURL

```bash
# Check a specific UUID
curl -X POST http://localhost:8080/bloom-filter/check \
  -H "Content-Type: application/json" \
  -d '{"item":"550e8400-e29b-41d4-a716-446655440000"}'

# Generate and check a random UUID
curl -X POST http://localhost:8080/bloom-filter/check-random

# Get statistics
curl -X GET http://localhost:8080/bloom-filter/stats

# Clear all data
curl -X DELETE http://localhost:8080/bloom-filter/clear
```

### Using JavaScript

```javascript
// Check duplicate
const response = await fetch('/bloom-filter/check', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ item: 'your-uuid-here' })
});

const result = await response.json();
console.log(`Is duplicate: ${result.isDuplicate}`);
```

## Testing

Run the tests with:

```bash
mvn test
```

The test suite includes:
- New item detection
- Duplicate detection
- False positive handling
- Statistics verification
- Error handling

## Deployment

### Development
```bash
mvn quarkus:dev
```

### Production
```bash
mvn clean package
java -jar target/quarkus-app/quarkus-run.jar
```

## Extending the System

### Adding New Backend Stores

1. Implement the `BackendStore` interface
2. Add the implementation to `BackendStoreConfig`
3. Configure the backend type in `application.properties`

### Example Redis Backend Store

```java
@ApplicationScoped
public class RedisBackendStore implements BackendStore {
    // Implementation using Redis client
}
```

## Monitoring and Metrics

The system provides statistics through the `/bloom-filter/stats` endpoint:
- Expected insertions capacity
- Current false positive rate
- Number of items in backend store
- Approximate bloom filter element count
- Backend store type

## Best Practices

1. **Choose Appropriate Capacity**: Set `expectedInsertions` based on your expected data volume
2. **Monitor False Positives**: Use the statistics endpoint to monitor false positive rates
3. **Backend Selection**: Use in-memory for development, PostgreSQL for production
4. **Cache Tuning**: Adjust cache size based on your false positive rate and memory constraints
5. **Regular Maintenance**: Clear old data periodically to maintain performance

## Troubleshooting

### High False Positive Rate
- Increase the `expectedInsertions` value
- Decrease the `falsePositiveRate` value
- Monitor cache hit rates

### Performance Issues
- Check cache configuration
- Monitor backend store performance
- Consider using a faster backend store

### Memory Issues
- Reduce cache size
- Use persistent backend store
- Monitor bloom filter memory usage

