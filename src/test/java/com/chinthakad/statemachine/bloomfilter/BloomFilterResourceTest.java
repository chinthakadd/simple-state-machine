package com.chinthakad.statemachine.bloomfilter;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class BloomFilterResourceTest {

    @BeforeEach
    void setUp() {
        // Clear the bloom filter before each test
        given()
            .when()
            .delete("/bloom-filter/clear")
            .then()
            .statusCode(200);
    }

    @Test
    void testCheckDuplicateWithNewItem() {
        String newItem = UUID.randomUUID().toString();
        
        given()
            .contentType(ContentType.JSON)
            .body("{\"item\":\"" + newItem + "\"}")
            .when()
            .post("/bloom-filter/check")
            .then()
            .statusCode(200)
            .body("item", equalTo(newItem))
            .body("isDuplicate", equalTo(false))
            .body("wasFalsePositive", equalTo(false))
            .body("message", equalTo("New item"));
    }

    @Test
    void testCheckDuplicateWithSameItem() {
        String item = UUID.randomUUID().toString();
        
        // First check - should be new
        given()
            .contentType(ContentType.JSON)
            .body("{\"item\":\"" + item + "\"}")
            .when()
            .post("/bloom-filter/check")
            .then()
            .statusCode(200)
            .body("isDuplicate", equalTo(false));
        
        // Second check - should be duplicate
        given()
            .contentType(ContentType.JSON)
            .body("{\"item\":\"" + item + "\"}")
            .when()
            .post("/bloom-filter/check")
            .then()
            .statusCode(200)
            .body("item", equalTo(item))
            .body("isDuplicate", equalTo(true))
            .body("message", equalTo("Confirmed duplicate"));
    }

    @Test
    void testCheckRandomDuplicate() {
        given()
            .when()
            .post("/bloom-filter/check-random")
            .then()
            .statusCode(200)
            .body("isDuplicate", equalTo(false))
            .body("wasFalsePositive", equalTo(false))
            .body("message", equalTo("New item"));
    }

    @Test
    void testCheckDuplicateWithEmptyItem() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"item\":\"\"}")
            .when()
            .post("/bloom-filter/check")
            .then()
            .statusCode(400)
            .body("error", equalTo("Item cannot be null or empty"));
    }

    @Test
    void testCheckDuplicateWithNullItem() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"item\":null}")
            .when()
            .post("/bloom-filter/check")
            .then()
            .statusCode(400)
            .body("error", equalTo("Item cannot be null or empty"));
    }

    @Test
    void testGetStats() {
        given()
            .when()
            .get("/bloom-filter/stats")
            .then()
            .statusCode(200)
            .body("expectedInsertions", equalTo(1000000))
            .body("falsePositiveRate", equalTo(0.01f))
            .body("backendStoreName", equalTo("InMemory"));
    }

    @Test
    void testClear() {
        given()
            .when()
            .delete("/bloom-filter/clear")
            .then()
            .statusCode(200)
            .body("message", equalTo("Bloom filter cleared successfully"));
    }

    @Test
    void testMultipleRandomChecks() {
        // Check multiple random items
        for (int i = 0; i < 5; i++) {
            given()
                .when()
                .post("/bloom-filter/check-random")
                .then()
                .statusCode(200)
                .body("isDuplicate", equalTo(false));
        }
        
        // Verify stats show items were added
        given()
            .when()
            .get("/bloom-filter/stats")
            .then()
            .statusCode(200)
            .body("backendCount", equalTo(5));
    }
}

