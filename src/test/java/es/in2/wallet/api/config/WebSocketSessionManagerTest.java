package es.in2.wallet.api.config;
import es.in2.wallet.infrastructure.core.config.WebSocketSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;

class WebSocketSessionManagerTest {

    @Mock
    private WebSocketSession session;

    private WebSocketSessionManager webSocketSessionManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        webSocketSessionManager = new WebSocketSessionManager();
    }

    @Test
    void testRegisterSession() {
        String userId = "user123";

        webSocketSessionManager.registerSession(userId, session);

        // Verificar que la sesión se ha registrado correctamente
        StepVerifier.create(webSocketSessionManager.getSession(userId))
                .expectNext(session)
                .verifyComplete();
    }

    @Test
    void testGetSessionWhenSessionExists() {
        String userId = "user123";
        WebSocketSession existingSession = mock(WebSocketSession.class);

        // Registrar la sesión
        webSocketSessionManager.registerSession(userId, existingSession);

        // Verificar que la sesión se obtiene correctamente
        StepVerifier.create(webSocketSessionManager.getSession(userId))
                .expectNext(existingSession)
                .verifyComplete();
    }
}

