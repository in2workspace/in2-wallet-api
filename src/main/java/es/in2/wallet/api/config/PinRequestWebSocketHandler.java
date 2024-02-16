package es.in2.wallet.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.api.model.ClientMessage;
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

import static es.in2.wallet.api.util.MessageUtils.getUserIdFromToken;

@Getter
@Component
@RequiredArgsConstructor
@Slf4j
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
                        ClientMessage clientMessage = objectMapper.readValue(message.getPayloadAsText(), ClientMessage.class);
                        String sessionId = session.getId();

                        // Case for handling JWT user token for session linkage
                        if (clientMessage.id() != null) {
                            return getUserIdFromToken(clientMessage.id())
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
                        else if (clientMessage.pin() != null) {
                            // Retrieve the user ID associated with the session ID
                            String userId = sessionToUserIdMap.get(sessionId);
                            if (userId != null) {
                                // Get the corresponding sink for the user
                                Sinks.Many<String> sink = pinSinks.get(userId);
                                if (sink != null) {
                                    // Emit the received PIN into the sink
                                    sink.tryEmitNext(clientMessage.pin());
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

    public void sendPinRequest(WebSocketSession session) {
        // Message to request the PIN from the client
        String requestMessage = "Pin required";
        // Send the message to the client via the WebSocket session
        session.send(Mono.just(session.textMessage(requestMessage))).subscribe();
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
