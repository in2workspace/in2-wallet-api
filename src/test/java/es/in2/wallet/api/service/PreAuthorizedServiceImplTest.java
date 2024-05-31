package es.in2.wallet.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.domain.exception.InvalidPinException;
import es.in2.wallet.domain.exception.ParseErrorException;
import es.in2.wallet.domain.model.AuthorisationServerMetadata;
import es.in2.wallet.domain.model.CredentialOffer;
import es.in2.wallet.domain.model.TokenResponse;
import es.in2.wallet.domain.service.impl.PreAuthorizedServiceImpl;
import es.in2.wallet.domain.util.ApplicationUtils;
import es.in2.wallet.infrastructure.core.config.PinRequestWebSocketHandler;
import es.in2.wallet.infrastructure.core.config.WebClientConfig;
import es.in2.wallet.infrastructure.core.config.WebSocketSessionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static es.in2.wallet.domain.util.ApplicationConstants.CONTENT_TYPE;
import static es.in2.wallet.domain.util.ApplicationConstants.CONTENT_TYPE_APPLICATION_JSON;
import static es.in2.wallet.domain.util.ApplicationUtils.getUserIdFromToken;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    private WebClientConfig webClientConfig;

    @Mock
    private WebSocketSession mockSession;


    @InjectMocks
    private PreAuthorizedServiceImpl tokenService;

    @Test
    void getPreAuthorizedTokenWithoutPinTest() throws JsonProcessingException {
            String processId = "123";
            String token = "ey123";
            CredentialOffer.Grant.PreAuthorizedCodeGrant preAuthorizedCodeGrant = CredentialOffer.Grant.PreAuthorizedCodeGrant.builder().preAuthorizedCode("321").build();
            CredentialOffer.Grant grant = CredentialOffer.Grant.builder().preAuthorizedCodeGrant(preAuthorizedCodeGrant).build();
            CredentialOffer credentialOffer = CredentialOffer.builder().grant(grant).build();
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().tokenEndpoint("/token").build();
            TokenResponse expectedTokenResponse = TokenResponse.builder().accessToken("example token").build();
            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                    .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                    .body("token response")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();

            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);
            when(objectMapper.readValue("token response", TokenResponse.class)).thenReturn(expectedTokenResponse);

            StepVerifier.create(tokenService.getPreAuthorizedToken(processId,credentialOffer,authorisationServerMetadata,token))
                    .expectNext(expectedTokenResponse)
                    .verifyComplete();
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

            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.BAD_REQUEST)
                    .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                    .body("error")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();

            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

            StepVerifier.create(tokenService.getPreAuthorizedToken(processId,credentialOffer,authorisationServerMetadata,token))
                    .expectError(InvalidPinException.class)
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

            CredentialOffer.Grant.PreAuthorizedCodeGrant preAuthorizedCodeGrant = CredentialOffer.Grant.PreAuthorizedCodeGrant.builder()
                    .preAuthorizedCode("321").userPinRequired(true).build();
            CredentialOffer.Grant grant = CredentialOffer.Grant.builder().preAuthorizedCodeGrant(preAuthorizedCodeGrant).build();
            CredentialOffer credentialOffer = CredentialOffer.builder().grant(grant).build();
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().tokenEndpoint("/token").build();
            TokenResponse expectedTokenResponse = TokenResponse.builder().accessToken("example token").build();

            when(getUserIdFromToken(token)).thenReturn(Mono.just(userId));
            when(sessionManager.getSession(userId)).thenReturn(Mono.just(mockSession));
            when(pinRequestWebSocketHandler.getPinResponses(userId)).thenReturn(Flux.just(userPin));

            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                    .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                    .body("token response")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();

            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

            when(objectMapper.readValue("token response", TokenResponse.class)).thenReturn(expectedTokenResponse);

            StepVerifier.create(tokenService.getPreAuthorizedToken(processId,credentialOffer,authorisationServerMetadata,token))
                    .expectNext(expectedTokenResponse)
                    .verifyComplete();
        }
    }
    @Test
    void getPreAuthorizedTokenWithPinParseErrorException () throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "123";
            String token = "ey123";
            String userId = "user123";
            String userPin = "1234";
            CredentialOffer.Grant.PreAuthorizedCodeGrant preAuthorizedCodeGrant = CredentialOffer.Grant.PreAuthorizedCodeGrant.builder()
                    .preAuthorizedCode("321").userPinRequired(true).build();
            CredentialOffer.Grant grant = CredentialOffer.Grant.builder().preAuthorizedCodeGrant(preAuthorizedCodeGrant).build();
            CredentialOffer credentialOffer = CredentialOffer.builder().grant(grant).build();
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().tokenEndpoint("/token").build();

            when(getUserIdFromToken(token)).thenReturn(Mono.just(userId));
            when(sessionManager.getSession(userId)).thenReturn(Mono.just(mockSession));
            when(pinRequestWebSocketHandler.getPinResponses(userId)).thenReturn(Flux.just(userPin));

            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                    .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                    .body("token response")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();

            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

            when(objectMapper.readValue("token response", TokenResponse.class)).thenThrow(new RuntimeException());

            StepVerifier.create(tokenService.getPreAuthorizedToken(processId,credentialOffer,authorisationServerMetadata,token))
                    .expectError(ParseErrorException.class)
                    .verify();
        }
    }

    @Test
    void getPreAuthorizedTokenWithTxCodeTest() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "123";
            String token = "ey123";
            String userId = "user123";
            String userPin = "1234";
            CredentialOffer.Grant.PreAuthorizedCodeGrant preAuthorizedCodeGrant = CredentialOffer.Grant.PreAuthorizedCodeGrant.builder()
                    .preAuthorizedCode("321").txCode(
                            CredentialOffer.Grant.PreAuthorizedCodeGrant.TxCode.builder().description("example").build()
                    ).build();
            CredentialOffer.Grant grant = CredentialOffer.Grant.builder().preAuthorizedCodeGrant(preAuthorizedCodeGrant).build();
            CredentialOffer credentialOffer = CredentialOffer.builder().grant(grant).build();
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().tokenEndpoint("/token").build();
            TokenResponse expectedTokenResponse = TokenResponse.builder().accessToken("example token").build();

            when(getUserIdFromToken(token)).thenReturn(Mono.just(userId));
            when(sessionManager.getSession(userId)).thenReturn(Mono.just(mockSession));
            when(pinRequestWebSocketHandler.getPinResponses(userId)).thenReturn(Flux.just(userPin));

            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                    .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                    .body("token response")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();

            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

            when(objectMapper.readValue("token response", TokenResponse.class)).thenReturn(expectedTokenResponse);

            StepVerifier.create(tokenService.getPreAuthorizedToken(processId,credentialOffer,authorisationServerMetadata,token))
                    .expectNext(expectedTokenResponse)
                    .verifyComplete();
        }
    }
}
