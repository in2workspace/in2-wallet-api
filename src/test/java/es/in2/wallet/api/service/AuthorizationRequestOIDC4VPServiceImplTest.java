package es.in2.wallet.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.domain.model.AuthorizationRequestOIDC4VP;
import es.in2.wallet.domain.service.impl.AuthorizationRequestServiceImpl;
import es.in2.wallet.infrastructure.core.config.WebClientConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static es.in2.wallet.domain.util.ApplicationConstants.CONTENT_TYPE;
import static es.in2.wallet.domain.util.ApplicationConstants.CONTENT_TYPE_APPLICATION_JSON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationRequestOIDC4VPServiceImplTest {

    @Mock
    private WebClientConfig webClientConfig;

    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private AuthorizationRequestServiceImpl authorizationRequestService;

    @Test
    void getAuthorizationRequestObjectFromUriTestRequestUri() {
            String processId = "123";
            String qrContent = "https://example/authentication-requests?request_uri=https://redirect.com";

            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                    .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                    .body("response")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);


            StepVerifier.create(authorizationRequestService.getJwtRequestObjectFromUri(processId,qrContent))
                    .expectNext("response")
                    .verifyComplete();

    }
    @Test
    void getAuthorizationRequestObjectFromUriTestRequest() {
        String processId = "123";
        String qrContent = "https://example/authentication-requests?request=ey1234...";

        StepVerifier.create(authorizationRequestService.getJwtRequestObjectFromUri(processId,qrContent))
                .expectNext("ey1234...")
                .verifyComplete();

    }

    @Test
    void getAuthorizationRequestFromVcLoginRequestErrorTest() {
            String processId = "123";
            String qrContent = "https://example/authentication-requests?request_uri=malformaturl";


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

            StepVerifier.create(authorizationRequestService.getJwtRequestObjectFromUri(processId,qrContent))
                    .expectError(RuntimeException.class)
                    .verify();

    }

    @Test
    void getAuthorizationRequestFromJwtAuthorizationRequestClaimTest() throws Exception {
        String processId = "123";
        String jwtAuthorizationRequest = "eyJraWQiOiJkaWQ6a2V5OnpEbmFla2l3a1djWG5IYVc2YXUzQnBtZldmcnRWVEpackEzRUhnTHZjYm02RVpudXAiLCJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJyZXNwb25zZV91cmkiOiJodHRwOi8vbG9jYWxob3N0OjkwMDAvb2lkNHZwL2F1dGgtcmVzcG9uc2UiLCJjbGllbnRfaWRfc2NoZW1lIjoiZGlkIiwiaXNzIjoiZGlkOmtleTp6RG5hZWtpd2tXY1huSGFXNmF1M0JwbWZXZnJ0VlRKWnJBM0VIZ0x2Y2JtNkVabnVwIiwicmVzcG9uc2VfdHlwZSI6InZwX3Rva2VuIiwibm9uY2UiOiI4ODU0YWMyNS0yZTFiLTQzYzktYjRiYi0yNmNhY2ZiNTg1ZTIiLCJjbGllbnRfaWQiOiJkaWQ6a2V5OnpEbmFla2l3a1djWG5IYVc2YXUzQnBtZldmcnRWVEpackEzRUhnTHZjYm02RVpudXAiLCJyZXNwb25zZV9tb2RlIjoiZGlyZWN0X3Bvc3QiLCJhdWQiOiJkaWQ6a2V5OnpEbmFla2l3a1djWG5IYVc2YXUzQnBtZldmcnRWVEpackEzRUhnTHZjYm02RVpudXAiLCJzY29wZSI6ImRvbWUuY3JlZGVudGlhbHMucHJlc2VudGF0aW9uLkxFQVJDcmVkZW50aWFsRW1wbG95ZWUiLCJzdGF0ZSI6IjEyMzQ1IiwiZXhwIjoxNzI4ODExNTU4LCJpYXQiOjE3Mjc5NDc1NTh9.WP0FDQXsIjsey9-ktJ5HAJFg9ItuFXL9brZDSIJJuT0YKKz5HpnAY2Dit9cSnZroq9gZnFY_kFm38OOjFUwx6w";

        // Simulación del payload mapeado desde JWT
        AuthorizationRequestOIDC4VP expectedAuthorizationRequest = AuthorizationRequestOIDC4VP.builder()
                .scope(List.of("dome.credentials.presentation.LEARCredentialEmployee"))
                .responseType("vp_token")
                .responseMode("direct_post")
                .clientId("did:key:zDnaekiwkWcXnHaW6au3BpmfWfrtVTJZrA3EHgLvcbm6EZnup")
                .state("12345")
                .nonce("8854ac25-2e1b-43c9-b4bb-26cacfb585e2")
                .build();

        String jsonString = "{\"response_uri\":\"http://localhost:9000/oid4vp/auth-response\",\"client_id_scheme\":\"did\",\"iss\":\"did:key:zDnaekiwkWcXnHaW6au3BpmfWfrtVTJZrA3EHgLvcbm6EZnup\",\"response_type\":\"vp_token\",\"nonce\":\"6a0ad2ea-00f0-498d-b9fd-117a3889d45a\",\"client_id\":\"did:key:zDnaekiwkWcXnHaW6au3BpmfWfrtVTJZrA3EHgLvcbm6EZnup\",\"response_mode\":\"direct_post\",\"aud\":\"did:key:zDnaekiwkWcXnHaW6au3BpmfWfrtVTJZrA3EHgLvcbm6EZnup\",\"scope\":[\"dome.credentials.presentation.LEARCredentialEmployee\"],\"state\":\"12345\",\"exp\":1728818405,\"iat\":1727954405}";

        when(objectMapper.writeValueAsString(any())).thenReturn(jsonString);
        when(objectMapper.readValue(jsonString, AuthorizationRequestOIDC4VP.class)).thenReturn(expectedAuthorizationRequest);

        Mono<AuthorizationRequestOIDC4VP> result = authorizationRequestService.getAuthorizationRequestFromJwtAuthorizationRequestJWT(processId, jwtAuthorizationRequest);

        // Verificación
        StepVerifier.create(result)
                .expectNextMatches(authRequest ->
                        authRequest.scope().contains("dome.credentials.presentation.LEARCredentialEmployee") &&
                                authRequest.responseType().equals("vp_token") &&
                                authRequest.responseMode().equals("direct_post") &&
                                authRequest.clientId().equals("did:key:zDnaekiwkWcXnHaW6au3BpmfWfrtVTJZrA3EHgLvcbm6EZnup") &&
                                authRequest.state().equals("12345") &&
                                authRequest.nonce().equals("8854ac25-2e1b-43c9-b4bb-26cacfb585e2")
                )
                .verifyComplete();
    }

    @Test
    void getAuthorizationRequestFromJwtAuthorizationRequestWithExceptionTest() throws Exception {
        String processId = "123";
        String jwtAuthorizationRequestClaim = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Error serializing") {});

        Mono<AuthorizationRequestOIDC4VP> result = authorizationRequestService.getAuthorizationRequestFromJwtAuthorizationRequestJWT(processId, jwtAuthorizationRequestClaim);

        // Verificación de que se lanza la excepción correctamente
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Error while parsing Authorization Request"))
                .verify();
    }

}
