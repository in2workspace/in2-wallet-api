package es.in2.wallet.broker.adapter;

import es.in2.wallet.infrastructure.broker.adapter.OrionLdAdapter;
import es.in2.wallet.infrastructure.broker.config.BrokerConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.lang.reflect.Field;

import static es.in2.wallet.domain.util.MessageUtils.ATTRIBUTES;
import static es.in2.wallet.domain.util.MessageUtils.ENTITY_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrionLdAdapterTest {

    @Mock
    private BrokerConfig brokerConfig;

    @Mock
    private MockWebServer mockWebServer;

    @InjectMocks
    private OrionLdAdapter orionLdAdapter;

    @BeforeEach
    void setUp() throws IOException, NoSuchFieldException, IllegalAccessException {
        // Mock the behavior of broker properties to return predefined paths
        when(brokerConfig.getPathEntities()).thenReturn("/entities");
        when(brokerConfig.getExternalUrl()).thenReturn("/external");

        // Initialize and start MockWebServer
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Initialize OrionLdAdapter with mocked properties
        orionLdAdapter = new OrionLdAdapter(brokerConfig);

        // Create a WebClient that points to the MockWebServer
        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        // Use reflection to inject the WebClient into OrionLdAdapter
        Field webClientField = OrionLdAdapter.class.getDeclaredField("webClient");
        webClientField.setAccessible(true);
        webClientField.set(orionLdAdapter, webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Shut down the server after each test
        mockWebServer.shutdown();
    }

    @Test
    void postEntityTest() throws Exception {
        // Prepare test data
        String processId = "processId123";
        String requestBody = "{\"key\":\"value\"}";

        // Enqueue a mock response for the POST request
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // Test the postEntity method
        StepVerifier.create(orionLdAdapter.postEntity(processId,requestBody))
                .verifyComplete(); // Verify the request completes successfully

        // Verify the POST request was made correctly
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/external/entities", recordedRequest.getPath());
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE));
        assertNotNull(recordedRequest.getBody().readUtf8()); // Ensure the request body was sent
    }

    @Test
    void getEntityByIdTest() throws Exception {
        // Prepare test data and mock response
        String userId = "userId123";
        String processId = "processId123";
        String expectedResponse = "{\"id\":\"entityId\"}";

        // Enqueue the mock response for the GET request
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(expectedResponse));

        // Test the getEntityById method
        StepVerifier.create(orionLdAdapter.getEntityById(processId, userId))
                .expectNextMatches(optionalResponse ->
                        optionalResponse.map(response -> response.contains("\"id\":\"entityId\""))
                                .orElse(false)) // Verify the response content within the Optional
                .verifyComplete();

        // Verify the GET request was made correctly
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/external/entities" + ENTITY_PREFIX + userId, recordedRequest.getPath());
        assertEquals("GET", recordedRequest.getMethod());
    }

    @Test
    void updateEntityTest() throws Exception {
        // Prepare test data and mock response
        String userId = "userId123";
        String processId = "processId123";
        String requestBody = "{\"newKey\":\"newValue\"}";

        // Enqueue a mock response for the PATCH request
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // Test the updateEntity method
        StepVerifier.create(orionLdAdapter.updateEntity(processId, userId, requestBody))
                .verifyComplete(); // Verify the request completes successfully

        // Verify the PATCH request was made correctly
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/external/entities" + ENTITY_PREFIX + userId + ATTRIBUTES, recordedRequest.getPath());
        assertEquals("PATCH", recordedRequest.getMethod());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE));
        assertNotNull(recordedRequest.getBody().readUtf8()); // Ensure the request body was sent
    }
}


