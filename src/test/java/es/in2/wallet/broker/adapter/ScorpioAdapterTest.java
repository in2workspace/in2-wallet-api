package es.in2.wallet.broker.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.broker.config.properties.BrokerConfig;
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
import java.util.Optional;

import static es.in2.wallet.api.util.MessageUtils.ATTRIBUTES;
import static es.in2.wallet.api.util.MessageUtils.ENTITY_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScorpioAdapterTest {

    @Mock
    private BrokerConfig brokerConfig;
    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MockWebServer mockWebServer;

    @InjectMocks
    private ScorpioAdapter scorpioAdapter;

    @BeforeEach
    void setUp() throws IOException, NoSuchFieldException, IllegalAccessException {
        // Mock the behavior of broker properties to return predefined paths
        when(brokerConfig.getPathEntities()).thenReturn("/entities");
        when(brokerConfig.getExternalUrl()).thenReturn("/external");

        // Initialize and start MockWebServer
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Initialize OrionLdAdapter with mocked properties
        scorpioAdapter = new ScorpioAdapter(objectMapper, brokerConfig);

        // Create a WebClient that points to the MockWebServer
        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        // Use reflection to inject the WebClient into OrionLdAdapter
        Field webClientField = ScorpioAdapter.class.getDeclaredField("webClient");
        webClientField.setAccessible(true);
        webClientField.set(scorpioAdapter, webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Shut down the server after each test
        mockWebServer.shutdown();
    }
    @Test
    void postEntityTestWithApplicationJson() throws Exception {
        // Prepare test data
        String processId = "processId123";
        String requestBody = "{\"key\":\"value\"}";

        ObjectMapper objectMapper1 = new ObjectMapper();
        JsonNode jsonNode = objectMapper1.readTree(requestBody);

        when(objectMapper.readTree(requestBody)).thenReturn(jsonNode);
        // Enqueue a mock response for the POST request
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // Test the postEntity method
        StepVerifier.create(scorpioAdapter.postEntity(processId, requestBody))
                .verifyComplete(); // Verify the request completes successfully

        // Verify the POST request was made correctly
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/external/entities", recordedRequest.getPath());
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE));
        assertNotNull(recordedRequest.getBody().readUtf8()); // Ensure the request body was sent
    }

    @Test
    void postEntityTestWithApplicationJsonLd() throws Exception {
        // Prepare test data
        String processId = "processId123";
        String requestBody = "{\"key\":\"value\", \"@context\": \"value\"}";

        ObjectMapper objectMapper1 = new ObjectMapper();
        JsonNode jsonNode = objectMapper1.readTree(requestBody);

        when(objectMapper.readTree(requestBody)).thenReturn(jsonNode);
        // Enqueue a mock response for the POST request
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // Test the postEntity method
        StepVerifier.create(scorpioAdapter.postEntity(processId, requestBody))
                .verifyComplete(); // Verify the request completes successfully

        // Verify the POST request was made correctly
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/external/entities", recordedRequest.getPath());
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals(MediaType.valueOf("application/ld+json").toString(), recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE));
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
        StepVerifier.create(scorpioAdapter.getEntityById(processId, userId))
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
    void updateEntityTestWithApplicationJson() throws Exception {
        // Prepare test data and mock response
        String userId = "userId123";
        String processId = "processId123";
        String requestBody = "{\"newKey\":\"newValue\"}";

        ObjectMapper objectMapper1 = new ObjectMapper();
        JsonNode jsonNode = objectMapper1.readTree(requestBody);

        when(objectMapper.readTree(requestBody)).thenReturn(jsonNode);
        // Enqueue a mock response for the PATCH request
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // Test the updateEntity method
        StepVerifier.create(scorpioAdapter.updateEntity(processId, userId, requestBody))
                .verifyComplete(); // Verify the request completes successfully

        // Verify the PATCH request was made correctly
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/external/entities" + ENTITY_PREFIX + userId + ATTRIBUTES, recordedRequest.getPath());
        assertEquals("PATCH", recordedRequest.getMethod());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE));
        assertNotNull(recordedRequest.getBody().readUtf8()); // Ensure the request body was sent
    }

    @Test
    void updateEntityTestWithApplicationJsonLd() throws Exception {
        // Prepare test data and mock response
        String userId = "userId123";
        String processId = "processId123";

        String requestBody = "{\"key\":\"value\", \"@context\": \"value\"}";

        ObjectMapper objectMapper1 = new ObjectMapper();
        JsonNode jsonNode = objectMapper1.readTree(requestBody);

        when(objectMapper.readTree(requestBody)).thenReturn(jsonNode);
        // Enqueue a mock response for the PATCH request
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // Test the updateEntity method
        StepVerifier.create(scorpioAdapter.updateEntity(processId, userId, requestBody))
                .verifyComplete(); // Verify the request completes successfully

        // Verify the PATCH request was made correctly
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/external/entities" + ENTITY_PREFIX + userId + ATTRIBUTES, recordedRequest.getPath());
        assertEquals("PATCH", recordedRequest.getMethod());
        assertEquals(MediaType.valueOf("application/ld+json").toString(), recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE));
        assertNotNull(recordedRequest.getBody().readUtf8()); // Ensure the request body was sent
    }
    @Test
    void getEntityByIdTestWithNotFoundResponse() throws Exception {
        String processId = "processId123";
        String userId = "123";

        // Enqueue a mock response with a 5xx server error
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        // Test the getEntityById method expecting an empty result
        StepVerifier.create(scorpioAdapter.getEntityById(processId,userId))
                .expectNextMatches(Optional::isEmpty) // Verify that an empty Optional is received
                .verifyComplete();

        // Verify the GET request was made correctly
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/external/entities" + ENTITY_PREFIX + userId, recordedRequest.getPath());
    }
}
