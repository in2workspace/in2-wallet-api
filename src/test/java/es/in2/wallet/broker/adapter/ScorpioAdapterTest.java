package es.in2.wallet.broker.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.infrastructure.broker.adapter.ScorpioAdapter;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static es.in2.wallet.domain.util.ApplicationConstants.ATTRIBUTES;
import static es.in2.wallet.domain.util.ApplicationConstants.USER_ENTITY_PREFIX;
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
        when(brokerConfig.getEntitiesPath()).thenReturn("/entities");
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
        StepVerifier.create(scorpioAdapter.getEntityById(processId, USER_ENTITY_PREFIX + userId))
                .expectNextMatches(optionalResponse ->
                        optionalResponse.map(response -> response.contains("\"id\":\"entityId\""))
                                .orElse(false)) // Verify the response content within the Optional
                .verifyComplete();

        // Verify the GET request was made correctly
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/external/entities" + "/" + USER_ENTITY_PREFIX + userId, recordedRequest.getPath());
        assertEquals("GET", recordedRequest.getMethod());
    }

    @Test
    void updateEntityTestWithApplicationJson() throws Exception {
        // Prepare test data and mock response
        String entityId = USER_ENTITY_PREFIX +  "123";
        String processId = "processId123";
        String requestBody = "{\"newKey\":\"newValue\"}";

        ObjectMapper objectMapper1 = new ObjectMapper();
        JsonNode jsonNode = objectMapper1.readTree(requestBody);

        when(objectMapper.readTree(requestBody)).thenReturn(jsonNode);
        // Enqueue a mock response for the POST request
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // Test the updateEntity method
        StepVerifier.create(scorpioAdapter.updateEntityById(processId, entityId, requestBody))
                .verifyComplete(); // Verify the request completes successfully

        // Verify the POST request was made correctly
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/external/entities" + "/" + entityId + ATTRIBUTES, recordedRequest.getPath());
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE));
        assertNotNull(recordedRequest.getBody().readUtf8()); // Ensure the request body was sent
    }

    @Test
    void updateEntityTestWithApplicationJsonLd() throws Exception {
        // Prepare test data and mock response
        String entityId = USER_ENTITY_PREFIX +  "123";
        String processId = "processId123";

        String requestBody = "{\"key\":\"value\", \"@context\": \"value\"}";

        ObjectMapper objectMapper1 = new ObjectMapper();
        JsonNode jsonNode = objectMapper1.readTree(requestBody);

        when(objectMapper.readTree(requestBody)).thenReturn(jsonNode);
        // Enqueue a mock response for the POST request
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // Test the updateEntity method
        StepVerifier.create(scorpioAdapter.updateEntityById(processId, entityId, requestBody))
                .verifyComplete(); // Verify the request completes successfully

        // Verify the POST request was made correctly
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/external/entities" + "/" + entityId + ATTRIBUTES, recordedRequest.getPath());
        assertEquals("POST", recordedRequest.getMethod());
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
        StepVerifier.create(scorpioAdapter.getEntityById(processId,USER_ENTITY_PREFIX + userId))
                .expectNextMatches(Optional::isEmpty) // Verify that an empty Optional is received
                .verifyComplete();

        // Verify the GET request was made correctly
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/external/entities" + "/" + USER_ENTITY_PREFIX + userId, recordedRequest.getPath());
    }

    @Test
    void getCredentialsThatBelongToUserTest() throws Exception {
        String processId = "processId123";
        String userId = "userId123";
        String entityId = USER_ENTITY_PREFIX + userId;
        String encodedQuery = URLEncoder.encode("belongsTo==", StandardCharsets.UTF_8) + entityId;

        String expectedResponse = "[{\"id\":\"credentialId1\"}, {\"id\":\"credentialId2\"}]";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(expectedResponse));

        StepVerifier.create(scorpioAdapter.getAllCredentialsByUserId(processId, userId))
                .expectNext(expectedResponse)
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/external/entities?type=Credential&q=" + encodedQuery, recordedRequest.getPath());
    }

    @Test
    void getCredentialByIdThatBelongToUserTest() throws Exception {
        String processId = "processId123";
        String userId = "userId123";
        String credentialId = "credentialId123";
        String entityId = USER_ENTITY_PREFIX + userId;
        String encodedQuery = URLEncoder.encode("belongsTo==", StandardCharsets.UTF_8) + entityId;

        String expectedResponse = "{\"id\":\"" + credentialId + "\", \"belongsTo\":\"" + entityId + "\"}";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(expectedResponse));

        StepVerifier.create(scorpioAdapter.getCredentialByIdAndUserId(processId, credentialId, userId))
                .expectNext(expectedResponse)
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/external/entities/" + credentialId + "?q=" + encodedQuery, recordedRequest.getPath());
    }

    @Test
    void deleteCredentialByIdThatBelongToUserTest() throws Exception {
        String processId = "processId123";
        String userId = "userId123";
        String credentialId = "credentialId123";
        String entityId = USER_ENTITY_PREFIX + userId;
        String encodedQuery = URLEncoder.encode("belongsTo==", StandardCharsets.UTF_8) + entityId;

        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        StepVerifier.create(scorpioAdapter.deleteCredentialByIdAndUserId(processId, credentialId, userId))
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/external/entities/" + credentialId + "?q=" + encodedQuery, recordedRequest.getPath());
        assertEquals("DELETE", recordedRequest.getMethod());
    }

    @Test
    void getCredentialByCredentialTypeThatBelongToUserTest() throws Exception {
        String processId = "processId123";
        String userId = "userId123";
        String credentialType = "Type123";
        String entityId = USER_ENTITY_PREFIX + userId;
        String encodedQuery = URLEncoder.encode("belongsTo==", StandardCharsets.UTF_8) + entityId +
                ";" + URLEncoder.encode("credentialType==", StandardCharsets.UTF_8) + credentialType;

        String expectedResponse = "[{\"type\":\"" + credentialType + "\", \"belongsTo\":\"" + entityId + "\"}]";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(expectedResponse));

        StepVerifier.create(scorpioAdapter.getCredentialByCredentialTypeAndUserId(processId, credentialType, userId))
                .expectNext(expectedResponse)
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/external/entities?type=Credential&q=" + encodedQuery, recordedRequest.getPath());
    }

    @Test
    void getTransactionThatIsLinkedToACredentialTest() throws Exception {
        String processId = "processId123";
        String credentialId = "credentialId123";
        String encodedQuery = URLEncoder.encode("linkedTo==", StandardCharsets.UTF_8) + credentialId;
        String expectedResponse = "[{\"transactionId\":\"tx123\", \"linkedTo\":\"" + credentialId + "\"}]";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(expectedResponse));

        StepVerifier.create(scorpioAdapter.getTransactionThatIsLinkedToACredential(processId, credentialId))
                .expectNext(expectedResponse)
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/external/entities?type=Transaction&q=" + encodedQuery, recordedRequest.getPath());
    }

    @Test
    void deleteTransactionByTransactionIdTest() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        String processId = "process1";
        String transactionId = "trans1";

        StepVerifier.create(scorpioAdapter.deleteTransactionByTransactionId(processId, transactionId))
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("DELETE", recordedRequest.getMethod());
        assertEquals("/external/entities/" + transactionId, recordedRequest.getPath());
    }


}
