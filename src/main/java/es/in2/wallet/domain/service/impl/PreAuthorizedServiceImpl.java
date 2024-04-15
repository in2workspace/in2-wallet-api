package es.in2.wallet.domain.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.domain.exception.InvalidPinException;
import es.in2.wallet.domain.model.AuthorisationServerMetadata;
import es.in2.wallet.domain.model.CredentialOffer;
import es.in2.wallet.domain.model.TokenResponse;
import es.in2.wallet.domain.service.PreAuthorizedService;
import es.in2.wallet.infrastructure.core.config.PinRequestWebSocketHandler;
import es.in2.wallet.infrastructure.core.config.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static es.in2.wallet.domain.util.ApplicationUtils.getUserIdFromToken;
import static es.in2.wallet.domain.util.ApplicationUtils.postRequest;
import static es.in2.wallet.domain.util.MessageUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreAuthorizedServiceImpl implements PreAuthorizedService {

    private final ObjectMapper objectMapper;
    private final WebSocketSessionManager sessionManager;
    private final PinRequestWebSocketHandler pinRequestWebSocketHandler;


    /**
     * Retrieves a pre-authorized token, potentially requiring a PIN from the user.
     *
     * @param processId The process identifier.
     * @param credentialOffer The credential offer containing grant information.
     * @param authorisationServerMetadata Metadata of the authorization server.
     * @param authorizationToken The JWT token used for authorization.
     * @return A Mono<TokenResponse> representing the token response.
     */
    @Override
    public Mono<TokenResponse> getPreAuthorizedToken(String processId, CredentialOffer credentialOffer,
                                                     AuthorisationServerMetadata authorisationServerMetadata,
                                                     String authorizationToken) {
        String tokenURL = authorisationServerMetadata.tokenEndpoint();

        if (credentialOffer.grant().preAuthorizedCodeGrant().userPinRequired()) {
            // If a user PIN is required, extract the user ID from the token,
            // send a PIN request, wait for the PIN response, and then proceed to get access token.
            return getUserIdFromToken(authorizationToken)
                    .flatMap(id -> sessionManager.getSession(id)
                            .flatMap(session -> {
                                pinRequestWebSocketHandler.sendPinRequest(session);
                                return waitForPinResponse(id);
                            })
                            .switchIfEmpty(Mono.error(new RuntimeException("WebSocket session not found")))
                            .flatMap(pin -> getAccessToken(tokenURL, credentialOffer, pin))
                            .flatMap(this::parseTokenResponse)
                    );
        } else {
            // If no PIN is required, directly proceed to get the access token.
            return getAccessToken(tokenURL, credentialOffer, null)
                    .flatMap(this::parseTokenResponse);
        }
    }

    /**
     * Waits for the PIN response from the user for a given user ID.
     *
     * @param userId The user ID for which to wait for the PIN response.
     * @return A Mono<String> representing the user's PIN.
     */
    private Mono<String> waitForPinResponse(String userId) {
        long timeoutDuration = 120; // Timeout duration in seconds
        // Waits for the next emitted PIN response and applies a timeout.
        return pinRequestWebSocketHandler.getPinResponses(userId)
                .next()
                .timeout(Duration.ofSeconds(timeoutDuration))
                .onErrorResume(TimeoutException.class, e -> Mono.error(new RuntimeException("Timeout waiting for PIN")));
    }

    private Mono<String> getAccessToken(String tokenURL, CredentialOffer credentialOffer, String pin) {
        // Headers
        List<Map.Entry<String, String>> headers = List.of(Map.entry(CONTENT_TYPE, CONTENT_TYPE_URL_ENCODED_FORM));

        // Build URL encoded form data request body
        Map<String, String> formDataMap = new HashMap<>();
        formDataMap.put("grant_type", PRE_AUTH_CODE_GRANT_TYPE);
        formDataMap.put("pre-authorized_code", credentialOffer.grant().preAuthorizedCodeGrant().preAuthorizedCode());
        if (pin != null && !pin.isEmpty()) {
            formDataMap.put("user_pin", pin);
        }

        String xWwwFormUrlencodedBody = formDataMap.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        // Post request
        return postRequest(tokenURL, headers, xWwwFormUrlencodedBody);
    }

    private Mono<TokenResponse> parseTokenResponse(String response) {
        try {
            return Mono.just(objectMapper.readValue(response, TokenResponse.class));
        } catch (Exception e) {
            return Mono.error(new InvalidPinException("Incorrect PIN" + e));
        }
    }

}
