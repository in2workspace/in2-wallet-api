package es.in2.wallet.api.service;

import es.in2.wallet.domain.model.AuthorizationRequest;
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

import static es.in2.wallet.domain.util.ApplicationConstants.CONTENT_TYPE;
import static es.in2.wallet.domain.util.ApplicationConstants.CONTENT_TYPE_APPLICATION_JSON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationRequestServiceImplTest {

    @Mock
    private WebClientConfig webClientConfig;
    @InjectMocks
    private AuthorizationRequestServiceImpl authorizationRequestService;

    @Test
    void getAuthorizationRequestFromVcLoginRequestTest() {
            String processId = "123";
            String qrContent = "https://example/authentication-requests?state=K00KRn2UT0ydwfs3A-5cZw";
            String authorizationToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJRQXpUVkRieUxsTVNzX2EwVFdrNy1JbGFDOTRnVjNPaDBDZWlKM2lydUIwIn0.eyJleHAiOjE3MDkwMTkyNTAsImlhdCI6MTcwOTAxODk1MCwianRpIjoiZTY5ZDI0YjQtYzA1OC00MTRmLTg0ZDYtNWZlY2RjMGU1Yzg4IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9FQUFQcm92aWRlciIsImF1ZCI6InJlYWxtLW1hbmFnZW1lbnQiLCJzdWIiOiIyZWI2NWY0ZC03YzE2LTRiMjUtYjNlZi04NDRlMjg3MmIyMDMiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJvaWRjNHZjaS1jbGllbnQiLCJzZXNzaW9uX3N0YXRlIjoiNGEyNTM2ZGEtYTQxNy00MjUzLWIzNGYtYzcyNjAzNjIzYjZlIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwczovL2VwdS1kZXYtYXBwLTAxLmF6dXJld2Vic2l0ZXMubmV0IiwiaHR0cDovL2xvY2FsaG9zdDo0MjAzIiwiaHR0cHM6Ly9lcHUtZGV2LWFwcC0wMy5henVyZXdlYnNpdGVzLm5ldCIsImh0dHA6Ly9sb2NhbGhvc3Q6NDIwMSIsImh0dHBzOi8vaXNzdWVyaWRwLmRldi5pbjIuZXMiLCJodHRwczovL3dhbGxldGlkcC5kZXYuaW4yLmVzIiwiaHR0cHM6Ly9lcHUtZGV2LWFwcC0wMi5henVyZXdlYnNpdGVzLm5ldCIsImh0dHBzOi8vaXNzdWVyZGV2LmluMi5lcy8qIiwiaHR0cDovL2xvY2FsaG9zdDo0MjAwIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJhZG1pbiIsImNyZWRlbnRpYWxfcmV2b2NhdGlvbl9hZG1pbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7InJlYWxtLW1hbmFnZW1lbnQiOnsicm9sZXMiOlsiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJ2aWV3LWF1dGhvcml6YXRpb24iLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJtYW5hZ2UtY2xpZW50cyIsInF1ZXJ5LWdyb3VwcyIsInF1ZXJ5LXVzZXJzIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwic2lkIjoiNGEyNTM2ZGEtYTQxNy00MjUzLWIzNGYtYzcyNjAzNjIzYjZlIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoiU3VwZXIgQWRtaW4iLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJpbjJhZG1pbjEiLCJnaXZlbl9uYW1lIjoiU3VwZXIiLCJmYW1pbHlfbmFtZSI6IkFkbWluIiwiZW1haWwiOiJhZG1pbnVzZXJAZXhhbXBsZS5jb20ifQ.R9abtB020XMxVwNeco2aUiwB1VjjyIpGdmLEyZXEMz_BAu6bcsohyfkny8I8d8UZ18-Wo2qfgXcZhDwrifGVr3YUnVP_nTJZx5liCM-yYXdnD5CRvSHSLueNk6iiQhE8cHEaJ5Y8oEd0jW0m4ci6XxJd68G5sM_1c3VxLz04mUqZ347n9Cq2B_sXyuN-U95-UAzoHNW23iGy7_Fqe9TCewQEtE7_1xMW3dO8_hCE0O1aUorqkBwWp1FPNQtgQi4bbEHPYI2rJbQXF6yrlagKcmexApiUU93thxulGylX-p64VJP82iw8EOfodiAxWndQk4uuSdVk29uz_AhuOl15fg";

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


            StepVerifier.create(authorizationRequestService.getAuthorizationRequestFromVcLoginRequest(processId,qrContent,authorizationToken))
                    .expectNext("response")
                    .verifyComplete();

    }

    @Test
    void getAuthorizationRequestFromVcLoginRequestErrorTest() {
            String processId = "123";
            String qrContent = "qrContentInvalid";
            String authorizationToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJRQXpUVkRieUxsTVNzX2EwVFdrNy1JbGFDOTRnVjNPaDBDZWlKM2lydUIwIn0.eyJleHAiOjE3MDkwMTkyNTAsImlhdCI6MTcwOTAxODk1MCwianRpIjoiZTY5ZDI0YjQtYzA1OC00MTRmLTg0ZDYtNWZlY2RjMGU1Yzg4IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9FQUFQcm92aWRlciIsImF1ZCI6InJlYWxtLW1hbmFnZW1lbnQiLCJzdWIiOiIyZWI2NWY0ZC03YzE2LTRiMjUtYjNlZi04NDRlMjg3MmIyMDMiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJvaWRjNHZjaS1jbGllbnQiLCJzZXNzaW9uX3N0YXRlIjoiNGEyNTM2ZGEtYTQxNy00MjUzLWIzNGYtYzcyNjAzNjIzYjZlIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwczovL2VwdS1kZXYtYXBwLTAxLmF6dXJld2Vic2l0ZXMubmV0IiwiaHR0cDovL2xvY2FsaG9zdDo0MjAzIiwiaHR0cHM6Ly9lcHUtZGV2LWFwcC0wMy5henVyZXdlYnNpdGVzLm5ldCIsImh0dHA6Ly9sb2NhbGhvc3Q6NDIwMSIsImh0dHBzOi8vaXNzdWVyaWRwLmRldi5pbjIuZXMiLCJodHRwczovL3dhbGxldGlkcC5kZXYuaW4yLmVzIiwiaHR0cHM6Ly9lcHUtZGV2LWFwcC0wMi5henVyZXdlYnNpdGVzLm5ldCIsImh0dHBzOi8vaXNzdWVyZGV2LmluMi5lcy8qIiwiaHR0cDovL2xvY2FsaG9zdDo0MjAwIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJhZG1pbiIsImNyZWRlbnRpYWxfcmV2b2NhdGlvbl9hZG1pbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7InJlYWxtLW1hbmFnZW1lbnQiOnsicm9sZXMiOlsiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJ2aWV3LWF1dGhvcml6YXRpb24iLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJtYW5hZ2UtY2xpZW50cyIsInF1ZXJ5LWdyb3VwcyIsInF1ZXJ5LXVzZXJzIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwic2lkIjoiNGEyNTM2ZGEtYTQxNy00MjUzLWIzNGYtYzcyNjAzNjIzYjZlIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoiU3VwZXIgQWRtaW4iLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJpbjJhZG1pbjEiLCJnaXZlbl9uYW1lIjoiU3VwZXIiLCJmYW1pbHlfbmFtZSI6IkFkbWluIiwiZW1haWwiOiJhZG1pbnVzZXJAZXhhbXBsZS5jb20ifQ.R9abtB020XMxVwNeco2aUiwB1VjjyIpGdmLEyZXEMz_BAu6bcsohyfkny8I8d8UZ18-Wo2qfgXcZhDwrifGVr3YUnVP_nTJZx5liCM-yYXdnD5CRvSHSLueNk6iiQhE8cHEaJ5Y8oEd0jW0m4ci6XxJd68G5sM_1c3VxLz04mUqZ347n9Cq2B_sXyuN-U95-UAzoHNW23iGy7_Fqe9TCewQEtE7_1xMW3dO8_hCE0O1aUorqkBwWp1FPNQtgQi4bbEHPYI2rJbQXF6yrlagKcmexApiUU93thxulGylX-p64VJP82iw8EOfodiAxWndQk4uuSdVk29uz_AhuOl15fg";

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

            StepVerifier.create(authorizationRequestService.getAuthorizationRequestFromVcLoginRequest(processId,qrContent,authorizationToken))
                    .expectError(RuntimeException.class)
                    .verify();

    }

    @Test
    void getAuthorizationRequestFromJwtAuthorizationRequestClaimTest() {
        String processId = "123";
        String jwtAuthorizationRequestClaim = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJhdXRoX3JlcXVlc3QiOiJodHRwOi8vZXhhbXBsZS5jb20vP3Njb3BlPWRhdGExLGRhdGEyLGRhdGEzJnJlc3BvbnNlX3R5cGU9dnBfdG9rZW4mcmVzcG9uc2VfbW9kZT1mb3JtX3Bvc3QmY2xpZW50X2lkPW15Q2xpZW50SWQmcmVkaXJlY3RfdXJpPWh0dHA6Ly9leGFtcGxlLmNvbS9yZWRpcmVjdCZzdGF0ZT1teVN0YXRlJm5vbmNlPW15Tm9uY2UifQ.PGO_Wx61hUcTFNndZIFM86KVMVQDl2y0vyUdHWbwvyg";

        Mono<AuthorizationRequest> result = authorizationRequestService.getAuthorizationRequestFromJwtAuthorizationRequestClaim(processId, jwtAuthorizationRequestClaim);

        StepVerifier.create(result)
                .expectNextMatches(authRequest ->
                        authRequest.scope().contains("data1") &&
                                authRequest.scope().contains("data2") &&
                                authRequest.scope().contains("data3") &&
                                authRequest.responseType().equals("vp_token") &&
                                authRequest.responseMode().equals("form_post") &&
                                authRequest.clientId().equals("myClientId") &&
                                authRequest.redirectUri().equals("http://example.com/redirect") &&
                                authRequest.state().equals("myState") &&
                                authRequest.nonce().equals("myNonce")
                )
                .verifyComplete();

    }

    @Test
    void getAuthorizationRequestFromJwtAuthorizationRequestWithoutClaimTest() {
        String processId = "123";
        String jwtAuthorizationRequestClaim = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        Mono<AuthorizationRequest> result = authorizationRequestService.getAuthorizationRequestFromJwtAuthorizationRequestClaim(processId,jwtAuthorizationRequestClaim);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

    }
}
