package org.apache.ignite.example.client;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.example.TestConfig;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class IgniteClientExampleIntegrationTest {

    private static IgniteClient client;

    @BeforeAll
    static void setup() {
        client = IgniteClient.builder()
                .addresses(TestConfig.HOST + ":" + TestConfig.PORT)
                .build();
    }

    @AfterAll
    static void teardown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void testConnection() {
        assertNotNull(client);
        assertNotNull(client.tables());
        // If cluster has no user tables this will still be valid
        assertNotNull(client.tables().tables());
    }

    @Test
    void testExampleMainRunsWithoutException() {
        assertDoesNotThrow(() -> IgniteClientExample.main(new String[]{}));
    }
}
