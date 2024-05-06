package es.in2.wallet.infrastructure.core.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.domain.exception.ParseErrorException;
import es.in2.wallet.domain.model.WebSocketClientMessage;
import es.in2.wallet.domain.model.WebSocketServerMessage;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static es.in2.wallet.domain.util.ApplicationUtils.getUserIdFromToken;

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class PinRequestWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final WebSocketSessionManager sessionManager;
    private final Map<String, Sinks.Many<String>> pinSinks = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToUserIdMap = new ConcurrentHashMap<>();

    @NotNull
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return session.receive()
                .flatMap(message -> {
                    try {
                        // Deserialize the incoming message to a ClientMessage object
                        WebSocketClientMessage webSocketClientMessage = objectMapper.readValue(message.getPayloadAsText(), WebSocketClientMessage.class);
                        String sessionId = session.getId();
                        // Case for handling JWT user token for session linkage
                        if (webSocketClientMessage.id() != null) {
                            return getUserIdFromToken(webSocketClientMessage.id())
                                    .doOnSuccess(userId -> {
                                        // Register the session with the extracted user ID
                                        sessionManager.registerSession(userId, session);
                                        // Initialize a sink for the user if not already present
                                        pinSinks.putIfAbsent(userId, Sinks.many().multicast().directBestEffort());
                                        // Map the session ID to the user ID for future reference
                                        sessionToUserIdMap.put(sessionId, userId);
                                    })
                                    .thenReturn(message.getPayloadAsText());
                        }
                        // Case for handling PIN messages
                        else if (webSocketClientMessage.pin() != null) {
                            // Retrieve the user ID associated with the session ID
                            String userId = sessionToUserIdMap.get(sessionId);
                            if (userId != null) {
                                // Get the corresponding sink for the user
                                Sinks.Many<String> sink = pinSinks.get(userId);
                                if (sink != null) {
                                    // Emit the received PIN into the sink
                                    sink.tryEmitNext(webSocketClientMessage.pin());
                                } else {
                                    log.error("Sink not found for user ID: " + userId);
                                }
                            } else {
                                log.error("User ID not found for session: " + sessionId);
                            }
                            return Mono.just(message.getPayloadAsText());
                        }
                        log.debug(message.getPayloadAsText());
                        return Mono.just(message.getPayloadAsText());
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Error processing message", e));
                    }
                })
                .then()
                .doFinally(signalType -> cleanUpResources(session));
    }

    public void sendPinRequest(WebSocketSession session, WebSocketServerMessage message) {
        try {
            // Serialize the message object to JSON
            String jsonMessage = objectMapper.writeValueAsString(message);

            // Send the message to the client via the WebSocket session
            session.send(Mono.just(session.textMessage(jsonMessage))).subscribe();
        } catch (JsonProcessingException e) {
            log.error("Error serializing WebSocketServerMessage", e);
            throw new ParseErrorException("Error serializing WebSocketServerMessage");
        }
    }


    public Flux<String> getPinResponses(String id) {
        // Retrieve the sink corresponding to the user ID and return its flux
        // This flux emits PINs received for the user
        return pinSinks.getOrDefault(id, Sinks.many().multicast().directBestEffort()).asFlux();
    }

    /**
     * Cleans up resources associated with a closed WebSocket session.
     * This method removes any session-related data to prevent memory leaks.
     *
     * @param session The WebSocket session that is being closed.
     */
    private void cleanUpResources(WebSocketSession session) {
        String sessionId = session.getId();
        String userId = sessionToUserIdMap.get(sessionId);
        // If a user ID is associated with the session, clean up resources
        if (userId != null) {
            // Remove the user's PIN sink
            pinSinks.remove(userId);
            // Remove the session-to-user ID mapping
            sessionToUserIdMap.remove(sessionId);
            log.debug("Cleaned up resources for session: " + sessionId + " and user: " + userId);
        }
    }

}
