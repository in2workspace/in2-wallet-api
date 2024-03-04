package es.in2.wallet.api.ebsi.comformance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.api.ebsi.comformance.service.impl.AuthorisationResponseServiceImpl;
import es.in2.wallet.api.exception.FailedCommunicationException;
import es.in2.wallet.api.model.AuthorisationServerMetadata;
import es.in2.wallet.api.model.TokenResponse;
import es.in2.wallet.api.util.ApplicationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static es.in2.wallet.api.util.ApplicationUtils.postRequest;
import static es.in2.wallet.api.util.MessageUtils.GLOBAL_STATE;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorisationResponseServiceImplTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuthorisationResponseServiceImpl authorisationResponseService;

    @Test
    void sendTokenRequest_SuccessfulFlow() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
            // Setup
            String codeVerifier = "codeVerifier";
            String did = "did:key:example";
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().tokenEndpoint("https://example/token").build();
            Map<String, String> params = Map.of("state", GLOBAL_STATE, "code", "authCode");

            TokenResponse expectedTokenResponse = TokenResponse.builder().accessToken("token").build();
            when(objectMapper.readValue(anyString(), eq(TokenResponse.class))).thenReturn(expectedTokenResponse);

            // Mock postRequest to simulate successful token response
            when(postRequest(eq(authorisationServerMetadata.tokenEndpoint()), anyList(), anyString())).thenReturn(Mono.just("token response"));

            // Execute & Verify
            StepVerifier.create(authorisationResponseService.sendTokenRequest(codeVerifier, did, authorisationServerMetadata, params))
                    .expectNext(expectedTokenResponse)
                    .verifyComplete();
        }
    }
    @Test
    void sendTokenRequest_FailedCommunicationException() {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
            // Setup
            String codeVerifier = "codeVerifier";
            String did = "did:key:example";
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().tokenEndpoint("https://example/token").build();
            Map<String, String> params = Map.of("state", GLOBAL_STATE, "code", "authCode");

            // Mock postRequest to simulate a failure in communication
            when(postRequest(eq(authorisationServerMetadata.tokenEndpoint()), anyList(), anyString()))
                    .thenReturn(Mono.error(new RuntimeException("Error during request")));

            // Execute & Verify
            StepVerifier.create(authorisationResponseService.sendTokenRequest(codeVerifier, did, authorisationServerMetadata, params))
                    .expectError(FailedCommunicationException.class)
                    .verify();
        }
    }

}
