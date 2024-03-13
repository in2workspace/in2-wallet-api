package es.in2.wallet.infrastructure.core.config;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class



WebSocketSessionManager {

    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    public void registerSession(String userId, WebSocketSession session) {
        userSessions.put(userId, session);
    }

    public Mono<WebSocketSession> getSession(String userId) {
        return Mono.just(userSessions.get(userId));
    }

}
