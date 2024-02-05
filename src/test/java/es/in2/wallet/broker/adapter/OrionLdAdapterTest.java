package es.in2.wallet.broker.adapter;

import es.in2.wallet.broker.properties.BrokerPathProperties;
import es.in2.wallet.broker.properties.BrokerProperties;
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

import static es.in2.wallet.api.util.MessageUtils.ATTRIBUTES;
import static es.in2.wallet.api.util.MessageUtils.ENTITY_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrionLdAdapterTest {

    @Mock
    private BrokerProperties brokerProperties;
    @Mock
    private BrokerPathProperties brokerPathProperties;

    @Mock
    private MockWebServer mockWebServer;

    @InjectMocks
    private OrionLdAdapter orionLdAdapter;

    @BeforeEach
    void setUp() throws IOException, NoSuchFieldException, IllegalAccessException {
        when(brokerPathProperties.entities()).thenReturn("/entities");
        when(brokerProperties.paths()).thenReturn(brokerPathProperties);
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        orionLdAdapter = new OrionLdAdapter(brokerProperties);

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        Field webClientField = OrionLdAdapter.class.getDeclaredField("webClient");
        webClientField.setAccessible(true);
        webClientField.set(orionLdAdapter, webClient);

    }
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    @Test
    void checkIfEntityAlreadyExistTest() throws Exception {
        String userId = "userId123";
        String processId = "processId123";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"id\":\"entityId\"}"));


        StepVerifier.create(orionLdAdapter.checkIfEntityAlreadyExist(processId, userId))
                .expectNext(true)
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/entities" + ENTITY_PREFIX + userId, recordedRequest.getPath());
        assertEquals("GET", recordedRequest.getMethod());
    }
    @Test
    void postEntityTest() throws Exception {
        String processId = "processId123";
        String authToken = "authToken123";
        String requestBody = "{\"key\":\"value\"}";

        // Configura MockWebServer para responder a la solicitud POST
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200));

        StepVerifier.create(orionLdAdapter.postEntity(processId, authToken, requestBody))
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/entities", recordedRequest.getPath());
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE));
        assertNotNull(recordedRequest.getBody().readUtf8());
    }
    @Test
    void getEntityByIdTest() throws Exception {
        String userId = "userId123";
        String processId = "processId123";
        String expectedResponse = "{\"id\":\"entityId\"}";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(expectedResponse));

        StepVerifier.create(orionLdAdapter.getEntityById(processId, userId))
                .expectNextMatches(response -> response.contains("\"id\":\"entityId\""))
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/entities" + ENTITY_PREFIX + userId, recordedRequest.getPath());
        assertEquals("GET", recordedRequest.getMethod());
    }
    @Test
    void updateEntityTest() throws Exception {
        String userId = "userId123";
        String processId = "processId123";
        String requestBody = "{\"newKey\":\"newValue\"}";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200));

        StepVerifier.create(orionLdAdapter.updateEntity(processId, userId, requestBody))
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/entities" + ENTITY_PREFIX + userId + ATTRIBUTES, recordedRequest.getPath());
        assertEquals("PATCH", recordedRequest.getMethod());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE));
        assertNotNull(recordedRequest.getBody().readUtf8());
    }




}

