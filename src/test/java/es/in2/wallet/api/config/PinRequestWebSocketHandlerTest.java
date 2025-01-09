package es.in2.wallet.api.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.domain.exception.ParseErrorException;
import es.in2.wallet.domain.model.WebSocketClientMessage;
import es.in2.wallet.domain.model.WebSocketServerMessage;
import es.in2.wallet.infrastructure.core.config.PinRequestWebSocketHandler;
import es.in2.wallet.infrastructure.core.config.WebSocketSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PinRequestWebSocketHandlerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebSocketSessionManager sessionManager;

    @InjectMocks
    private PinRequestWebSocketHandler handler;

    @Mock
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        handler = new PinRequestWebSocketHandler(objectMapper, sessionManager);
    }

    @Test
    void testHandleIdMessage() throws Exception {
        String jwtToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJxOGFyVmZaZTJpQkJoaU56RURnT3c3Tlc1ZmZHNElLTEtOSmVIOFQxdjJNIn0.eyJleHAiOjE3MTgzNjU3MjUsImlhdCI6MTcxODM2NTQyNSwiYXV0aF90aW1lIjoxNzE4MzUyODA1LCJqdGkiOiJlZWFmNWRlNy0wODc5LTRkYTktOGMwYS0yMGIzZDIwNWZjNGIiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjcwMDIvcmVhbG1zL3dhbGxldCIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiIyYzk5NTFkMi04NmNjLTQ0ZGYtOGQ2Mi0zNDIyN2NmYmVmOWMiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJhdXRoLWNsaWVudCIsIm5vbmNlIjoiYjVkZGVhZDE3ZGU2YjhmNzkyZDZkN2MwMzY4NTFlZjU3MGdRRjlxdDIiLCJzZXNzaW9uX3N0YXRlIjoiNjBkYjRiM2UtM2MzMi00NGY2LTk0YzItZGEzOGYyNTFmODc5IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vbG9jYWxob3N0OjQyMDIiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iLCJ1c2VyIiwiZGVmYXVsdC1yb2xlcy13YWxsZXQiXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBvZmZsaW5lX2FjY2VzcyBlbWFpbCBwcm9maWxlIiwic2lkIjoiNjBkYjRiM2UtM2MzMi00NGY2LTk0YzItZGEzOGYyNTFmODc5IiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoidXNlciB3YWxsZXQiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ1c2VyIiwiZ2l2ZW5fbmFtZSI6InVzZXIiLCJmYW1pbHlfbmFtZSI6IndhbGxldCIsImVtYWlsIjoidXNlcndhbGxldEBleGFtcGxlLmNvbSJ9.iQCM2Yxlw68-6r2aIM1XAU9aT_fK7dMOliwTX_wwZhORmk3D8qkFBLfg_6JnWyFE0lRYq_NP__mJZXneXFbnjWkXsEN4WyuuIzb-jRc1REu9A0b40N3Gt-JfjU1GEKw-4SkrG8tUsgM6lxCI0DEP1_V9z47YwDRkT50DzdtwBMa7aKQ3f3o3Cla_fCG2c0CKk6LsCYi9wOth2dEknRhqaEwwk1BXopsScE1hqB-evY-sYjETEK081tXaAbk5Mdsbp7tdWTsRoVhaDGSOB6ZzlKVscGP8KWPjD6DSmKfEGaLG7X8lKXMqhMaeT9UpgXGtWzi7Ey9E7OstB0APLhaoEA";

        String payload = """
                {
                    "id": "%s"
                }
                """.formatted(jwtToken);

        WebSocketClientMessage message = new WebSocketClientMessage(jwtToken, null);
        WebSocketMessage webSocketMessage = mock(WebSocketMessage.class);

        when(webSocketMessage.getPayloadAsText()).thenReturn(payload);
        when(session.receive()).thenReturn(Flux.just(webSocketMessage));
        when(objectMapper.readValue(payload, WebSocketClientMessage.class)).thenReturn(message);
        when(session.getId()).thenReturn("sessionId");

        StepVerifier.create(handler.handle(session))
                .verifyComplete();
    }

    @Test
    void testHandlePinMessage() throws Exception {
        String payload = "{\"pin\":\"1234\"}";
        WebSocketClientMessage message = new WebSocketClientMessage(null, "1234");
        WebSocketMessage webSocketMessage = mock(WebSocketMessage.class);

        when(webSocketMessage.getPayloadAsText()).thenReturn(payload);
        when(session.receive()).thenReturn(Flux.just(webSocketMessage));
        when(objectMapper.readValue(payload, WebSocketClientMessage.class)).thenReturn(message);
        when(session.getId()).thenReturn("sessionId");

        handler.getSessionToUserIdMap().put("sessionId", "testUser");
        Sinks.Many<String> sink = Sinks.many().multicast().directBestEffort();
        handler.getPinSinks().put("testUser", sink);

        StepVerifier.create(handler.handle(session))
                .then(sink::tryEmitComplete)
                .verifyComplete();

        StepVerifier.create(sink.asFlux())
                .verifyComplete();
    }


    @Test
    void testSendPinRequest() throws JsonProcessingException {
        WebSocketServerMessage serverMessage = new WebSocketServerMessage(null,true, 0);
        WebSocketMessage webSocketMessage = mock(WebSocketMessage.class);

        String jsonMessage = "{\"pin\":\"true\"}";

        when(objectMapper.writeValueAsString(serverMessage)).thenReturn(jsonMessage);
        when(session.textMessage(jsonMessage)).thenReturn(webSocketMessage);
        when(session.send(any())).thenReturn(Mono.empty());

        handler.sendPinRequest(session, serverMessage);

        verify(session, times(1)).send(any());
    }

    @Test
    void testSendPinRequestSerializationError() throws JsonProcessingException {
        WebSocketServerMessage serverMessage = new WebSocketServerMessage(null,true, 0);

        when(objectMapper.writeValueAsString(serverMessage)).thenThrow(JsonProcessingException.class);

        assertThrows(ParseErrorException.class, () -> handler.sendPinRequest(session, serverMessage));

        verify(session, never()).send(any());
    }

    @Test
    void testGetPinResponses() {
        String userId = "testUser";
        Sinks.Many<String> sink = Sinks.many().multicast().directBestEffort();
        handler.getPinSinks().put(userId, sink);

        Flux<String> pinResponses = handler.getPinResponses(userId);

        StepVerifier.create(pinResponses)
                .then(() -> sink.tryEmitNext("1234"))
                .expectNext("1234")
                .thenCancel()
                .verify();
    }

    @Test
    void testGetPinResponsesNoSink() {
        String userId = "unknownUser";

        Flux<String> pinResponses = handler.getPinResponses(userId);

        StepVerifier.create(pinResponses)
                .thenCancel()
                .verify();
    }
}

