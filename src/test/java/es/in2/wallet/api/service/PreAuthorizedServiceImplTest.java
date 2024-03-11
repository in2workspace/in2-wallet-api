package es.in2.wallet.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.infrastructure.core.config.PinRequestWebSocketHandler;
import es.in2.wallet.infrastructure.core.config.WebSocketSessionManager;
import es.in2.wallet.domain.model.AuthorisationServerMetadata;
import es.in2.wallet.domain.model.CredentialOffer;
import es.in2.wallet.domain.model.TokenResponse;
import es.in2.wallet.domain.service.impl.PreAuthorizedServiceImpl;
import es.in2.wallet.domain.util.ApplicationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static es.in2.wallet.domain.util.ApplicationUtils.getUserIdFromToken;
import static es.in2.wallet.domain.util.ApplicationUtils.postRequest;
import static es.in2.wallet.domain.util.MessageUtils.CONTENT_TYPE;
import static es.in2.wallet.domain.util.MessageUtils.CONTENT_TYPE_URL_ENCODED_FORM;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreAuthorizedServiceImplTest {
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private PinRequestWebSocketHandler pinRequestWebSocketHandler;

    @Mock
    private WebSocketSession mockSession;


    @InjectMocks
    private PreAuthorizedServiceImpl tokenService;

    @Test
    void getPreAuthorizedTokenWithoutPinTest() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "123";
            String token = "ey123";
            CredentialOffer.Grant.PreAuthorizedCodeGrant preAuthorizedCodeGrant = CredentialOffer.Grant.PreAuthorizedCodeGrant.builder().preAuthorizedCode("321").build();
            CredentialOffer.Grant grant = CredentialOffer.Grant.builder().preAuthorizedCodeGrant(preAuthorizedCodeGrant).build();
            CredentialOffer credentialOffer = CredentialOffer.builder().grant(grant).build();
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().tokenEndpoint("/token").build();
            TokenResponse expectedTokenResponse = TokenResponse.builder().accessToken("example token").build();
            List<Map.Entry<String, String>> headers = List.of(Map.entry(CONTENT_TYPE, CONTENT_TYPE_URL_ENCODED_FORM));

            when(postRequest(eq(authorisationServerMetadata.tokenEndpoint()), eq(headers), anyString()))
                    .thenReturn(Mono.just("token response"));
            when(objectMapper.readValue("token response", TokenResponse.class)).thenReturn(expectedTokenResponse);

            StepVerifier.create(tokenService.getPreAuthorizedToken(processId,credentialOffer,authorisationServerMetadata,token))
                    .expectNext(expectedTokenResponse)
                    .verifyComplete();
        }
    }
    @Test
    void getPreAuthorizedTokenWithoutPinExceptionTest(){
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "123";
            String token = "ey123";
            CredentialOffer.Grant.PreAuthorizedCodeGrant preAuthorizedCodeGrant = CredentialOffer.Grant.PreAuthorizedCodeGrant.builder().preAuthorizedCode("321").build();
            CredentialOffer.Grant grant = CredentialOffer.Grant.builder().preAuthorizedCodeGrant(preAuthorizedCodeGrant).build();
            CredentialOffer credentialOffer = CredentialOffer.builder().grant(grant).build();
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().tokenEndpoint("/token").build();
            List<Map.Entry<String, String>> headers = List.of(Map.entry(CONTENT_TYPE, CONTENT_TYPE_URL_ENCODED_FORM));

            when(postRequest(eq(authorisationServerMetadata.tokenEndpoint()), eq(headers), anyString())).thenReturn(Mono.error(new RuntimeException()));

            StepVerifier.create(tokenService.getPreAuthorizedToken(processId,credentialOffer,authorisationServerMetadata,token))
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }
    @Test
    void getPreAuthorizedTokenWithPinTest() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "123";
            String token = "ey123";
            String userId = "user123";
            String userPin = "1234";
            String tokenResponseString = "token response";
            CredentialOffer.Grant.PreAuthorizedCodeGrant preAuthorizedCodeGrant = CredentialOffer.Grant.PreAuthorizedCodeGrant.builder()
                    .preAuthorizedCode("321").userPinRequired(true).build();
            CredentialOffer.Grant grant = CredentialOffer.Grant.builder().preAuthorizedCodeGrant(preAuthorizedCodeGrant).build();
            CredentialOffer credentialOffer = CredentialOffer.builder().grant(grant).build();
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().tokenEndpoint("/token").build();
            TokenResponse expectedTokenResponse = TokenResponse.builder().accessToken("example token").build();
            List<Map.Entry<String, String>> headers = List.of(Map.entry(CONTENT_TYPE, CONTENT_TYPE_URL_ENCODED_FORM));

            when(getUserIdFromToken(token)).thenReturn(Mono.just(userId));
            when(sessionManager.getSession(userId)).thenReturn(Mono.just(mockSession));
            when(pinRequestWebSocketHandler.getPinResponses(userId)).thenReturn(Flux.just(userPin));
            when(postRequest(eq(authorisationServerMetadata.tokenEndpoint()), eq(headers), anyString()))
                    .thenReturn(Mono.just(tokenResponseString));
            when(objectMapper.readValue(tokenResponseString, TokenResponse.class)).thenReturn(expectedTokenResponse);

            StepVerifier.create(tokenService.getPreAuthorizedToken(processId,credentialOffer,authorisationServerMetadata,token))
                    .expectNext(expectedTokenResponse)
                    .verifyComplete();
        }
    }

}
