package es.in2.wallet.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.api.exception.FailedDeserializingException;
import es.in2.wallet.api.model.CredentialOffer;
import es.in2.wallet.api.service.impl.CredentialOfferServiceImpl;
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

import java.util.List;
import java.util.Map;

import static es.in2.wallet.api.util.ApplicationUtils.getRequest;
import static es.in2.wallet.api.util.MessageUtils.*;
import static es.in2.wallet.api.util.MessageUtils.BEARER;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialOfferServiceImplTest {
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private CredentialOfferServiceImpl credentialOfferService;
    @Test
    void getCredentialOfferTest() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "123";
            String qrContent = "openid-credential-offer://?credential_offer_uri=https%3A%2F%2Fapi-conformance.ebsi.eu%2Fconformance%2Fv3%2Fissuer-mock%2Foffers%2F00067a17-681f-4b41-9794-cb7c98570a7a";
            String authorizationToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJRQXpUVkRieUxsTVNzX2EwVFdrNy1JbGFDOTRnVjNPaDBDZWlKM2lydUIwIn0.eyJleHAiOjE3MDkwMTkyNTAsImlhdCI6MTcwOTAxODk1MCwianRpIjoiZTY5ZDI0YjQtYzA1OC00MTRmLTg0ZDYtNWZlY2RjMGU1Yzg4IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9FQUFQcm92aWRlciIsImF1ZCI6InJlYWxtLW1hbmFnZW1lbnQiLCJzdWIiOiIyZWI2NWY0ZC03YzE2LTRiMjUtYjNlZi04NDRlMjg3MmIyMDMiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJvaWRjNHZjaS1jbGllbnQiLCJzZXNzaW9uX3N0YXRlIjoiNGEyNTM2ZGEtYTQxNy00MjUzLWIzNGYtYzcyNjAzNjIzYjZlIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwczovL2VwdS1kZXYtYXBwLTAxLmF6dXJld2Vic2l0ZXMubmV0IiwiaHR0cDovL2xvY2FsaG9zdDo0MjAzIiwiaHR0cHM6Ly9lcHUtZGV2LWFwcC0wMy5henVyZXdlYnNpdGVzLm5ldCIsImh0dHA6Ly9sb2NhbGhvc3Q6NDIwMSIsImh0dHBzOi8vaXNzdWVyaWRwLmRldi5pbjIuZXMiLCJodHRwczovL3dhbGxldGlkcC5kZXYuaW4yLmVzIiwiaHR0cHM6Ly9lcHUtZGV2LWFwcC0wMi5henVyZXdlYnNpdGVzLm5ldCIsImh0dHBzOi8vaXNzdWVyZGV2LmluMi5lcy8qIiwiaHR0cDovL2xvY2FsaG9zdDo0MjAwIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJhZG1pbiIsImNyZWRlbnRpYWxfcmV2b2NhdGlvbl9hZG1pbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7InJlYWxtLW1hbmFnZW1lbnQiOnsicm9sZXMiOlsiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJ2aWV3LWF1dGhvcml6YXRpb24iLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJtYW5hZ2UtY2xpZW50cyIsInF1ZXJ5LWdyb3VwcyIsInF1ZXJ5LXVzZXJzIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwic2lkIjoiNGEyNTM2ZGEtYTQxNy00MjUzLWIzNGYtYzcyNjAzNjIzYjZlIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoiU3VwZXIgQWRtaW4iLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJpbjJhZG1pbjEiLCJnaXZlbl9uYW1lIjoiU3VwZXIiLCJmYW1pbHlfbmFtZSI6IkFkbWluIiwiZW1haWwiOiJhZG1pbnVzZXJAZXhhbXBsZS5jb20ifQ.R9abtB020XMxVwNeco2aUiwB1VjjyIpGdmLEyZXEMz_BAu6bcsohyfkny8I8d8UZ18-Wo2qfgXcZhDwrifGVr3YUnVP_nTJZx5liCM-yYXdnD5CRvSHSLueNk6iiQhE8cHEaJ5Y8oEd0jW0m4ci6XxJd68G5sM_1c3VxLz04mUqZ347n9Cq2B_sXyuN-U95-UAzoHNW23iGy7_Fqe9TCewQEtE7_1xMW3dO8_hCE0O1aUorqkBwWp1FPNQtgQi4bbEHPYI2rJbQXF6yrlagKcmexApiUU93thxulGylX-p64VJP82iw8EOfodiAxWndQk4uuSdVk29uz_AhuOl15fg";
            List<Map.Entry<String, String>> headers = List.of(Map.entry(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON),
                    Map.entry(HEADER_AUTHORIZATION, BEARER + authorizationToken));
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().format("jwt_vc").build();
            CredentialOffer expectedCredentialOffer = CredentialOffer.builder().credentialIssuer("https://example.com").credentials(List.of(credential)).build();

            when(getRequest("https://api-conformance.ebsi.eu/conformance/v3/issuer-mock/offers/00067a17-681f-4b41-9794-cb7c98570a7a",headers)).thenReturn(Mono.just("response"));

            when(objectMapper.readValue("response", CredentialOffer.class)).thenReturn(expectedCredentialOffer);

            StepVerifier.create(credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId,qrContent))
                    .expectNext(expectedCredentialOffer)
                    .verifyComplete();

        }
    }

    @Test
    void getCredentialOfferAlreadyParsedTest() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "123";
            String qrContent = "https://api-conformance.ebsi.eu/conformance/v3/issuer-mock/offers/00067a17-681f-4b41-9794-cb7c98570a7a";
            String authorizationToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJRQXpUVkRieUxsTVNzX2EwVFdrNy1JbGFDOTRnVjNPaDBDZWlKM2lydUIwIn0.eyJleHAiOjE3MDkwMTkyNTAsImlhdCI6MTcwOTAxODk1MCwianRpIjoiZTY5ZDI0YjQtYzA1OC00MTRmLTg0ZDYtNWZlY2RjMGU1Yzg4IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9FQUFQcm92aWRlciIsImF1ZCI6InJlYWxtLW1hbmFnZW1lbnQiLCJzdWIiOiIyZWI2NWY0ZC03YzE2LTRiMjUtYjNlZi04NDRlMjg3MmIyMDMiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJvaWRjNHZjaS1jbGllbnQiLCJzZXNzaW9uX3N0YXRlIjoiNGEyNTM2ZGEtYTQxNy00MjUzLWIzNGYtYzcyNjAzNjIzYjZlIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwczovL2VwdS1kZXYtYXBwLTAxLmF6dXJld2Vic2l0ZXMubmV0IiwiaHR0cDovL2xvY2FsaG9zdDo0MjAzIiwiaHR0cHM6Ly9lcHUtZGV2LWFwcC0wMy5henVyZXdlYnNpdGVzLm5ldCIsImh0dHA6Ly9sb2NhbGhvc3Q6NDIwMSIsImh0dHBzOi8vaXNzdWVyaWRwLmRldi5pbjIuZXMiLCJodHRwczovL3dhbGxldGlkcC5kZXYuaW4yLmVzIiwiaHR0cHM6Ly9lcHUtZGV2LWFwcC0wMi5henVyZXdlYnNpdGVzLm5ldCIsImh0dHBzOi8vaXNzdWVyZGV2LmluMi5lcy8qIiwiaHR0cDovL2xvY2FsaG9zdDo0MjAwIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJhZG1pbiIsImNyZWRlbnRpYWxfcmV2b2NhdGlvbl9hZG1pbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7InJlYWxtLW1hbmFnZW1lbnQiOnsicm9sZXMiOlsiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJ2aWV3LWF1dGhvcml6YXRpb24iLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJtYW5hZ2UtY2xpZW50cyIsInF1ZXJ5LWdyb3VwcyIsInF1ZXJ5LXVzZXJzIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwic2lkIjoiNGEyNTM2ZGEtYTQxNy00MjUzLWIzNGYtYzcyNjAzNjIzYjZlIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoiU3VwZXIgQWRtaW4iLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJpbjJhZG1pbjEiLCJnaXZlbl9uYW1lIjoiU3VwZXIiLCJmYW1pbHlfbmFtZSI6IkFkbWluIiwiZW1haWwiOiJhZG1pbnVzZXJAZXhhbXBsZS5jb20ifQ.R9abtB020XMxVwNeco2aUiwB1VjjyIpGdmLEyZXEMz_BAu6bcsohyfkny8I8d8UZ18-Wo2qfgXcZhDwrifGVr3YUnVP_nTJZx5liCM-yYXdnD5CRvSHSLueNk6iiQhE8cHEaJ5Y8oEd0jW0m4ci6XxJd68G5sM_1c3VxLz04mUqZ347n9Cq2B_sXyuN-U95-UAzoHNW23iGy7_Fqe9TCewQEtE7_1xMW3dO8_hCE0O1aUorqkBwWp1FPNQtgQi4bbEHPYI2rJbQXF6yrlagKcmexApiUU93thxulGylX-p64VJP82iw8EOfodiAxWndQk4uuSdVk29uz_AhuOl15fg";
            List<Map.Entry<String, String>> headers = List.of(Map.entry(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON),
                    Map.entry(HEADER_AUTHORIZATION, BEARER + authorizationToken));
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().format("jwt_vc").build();
            CredentialOffer expectedCredentialOffer = CredentialOffer.builder().credentialIssuer("https://example.com").credentials(List.of(credential)).build();

            when(getRequest("https://api-conformance.ebsi.eu/conformance/v3/issuer-mock/offers/00067a17-681f-4b41-9794-cb7c98570a7a",headers)).thenReturn(Mono.just("response"));

            when(objectMapper.readValue("response", CredentialOffer.class)).thenReturn(expectedCredentialOffer);

            StepVerifier.create(credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId,qrContent))
                    .expectNext(expectedCredentialOffer)
                    .verifyComplete();

        }
    }

    @Test
    void getCredentialOfferFailedDeserializingException() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "123";
            String qrContent = "openid-credential-offer://?credential_offer_uri=https%3A%2F%2Fapi-conformance.ebsi.eu%2Fconformance%2Fv3%2Fissuer-mock%2Foffers%2F00067a17-681f-4b41-9794-cb7c98570a7a";
            String authorizationToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJRQXpUVkRieUxsTVNzX2EwVFdrNy1JbGFDOTRnVjNPaDBDZWlKM2lydUIwIn0.eyJleHAiOjE3MDkwMTkyNTAsImlhdCI6MTcwOTAxODk1MCwianRpIjoiZTY5ZDI0YjQtYzA1OC00MTRmLTg0ZDYtNWZlY2RjMGU1Yzg4IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9FQUFQcm92aWRlciIsImF1ZCI6InJlYWxtLW1hbmFnZW1lbnQiLCJzdWIiOiIyZWI2NWY0ZC03YzE2LTRiMjUtYjNlZi04NDRlMjg3MmIyMDMiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJvaWRjNHZjaS1jbGllbnQiLCJzZXNzaW9uX3N0YXRlIjoiNGEyNTM2ZGEtYTQxNy00MjUzLWIzNGYtYzcyNjAzNjIzYjZlIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwczovL2VwdS1kZXYtYXBwLTAxLmF6dXJld2Vic2l0ZXMubmV0IiwiaHR0cDovL2xvY2FsaG9zdDo0MjAzIiwiaHR0cHM6Ly9lcHUtZGV2LWFwcC0wMy5henVyZXdlYnNpdGVzLm5ldCIsImh0dHA6Ly9sb2NhbGhvc3Q6NDIwMSIsImh0dHBzOi8vaXNzdWVyaWRwLmRldi5pbjIuZXMiLCJodHRwczovL3dhbGxldGlkcC5kZXYuaW4yLmVzIiwiaHR0cHM6Ly9lcHUtZGV2LWFwcC0wMi5henVyZXdlYnNpdGVzLm5ldCIsImh0dHBzOi8vaXNzdWVyZGV2LmluMi5lcy8qIiwiaHR0cDovL2xvY2FsaG9zdDo0MjAwIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJhZG1pbiIsImNyZWRlbnRpYWxfcmV2b2NhdGlvbl9hZG1pbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7InJlYWxtLW1hbmFnZW1lbnQiOnsicm9sZXMiOlsiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJ2aWV3LWF1dGhvcml6YXRpb24iLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJtYW5hZ2UtY2xpZW50cyIsInF1ZXJ5LWdyb3VwcyIsInF1ZXJ5LXVzZXJzIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwic2lkIjoiNGEyNTM2ZGEtYTQxNy00MjUzLWIzNGYtYzcyNjAzNjIzYjZlIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoiU3VwZXIgQWRtaW4iLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJpbjJhZG1pbjEiLCJnaXZlbl9uYW1lIjoiU3VwZXIiLCJmYW1pbHlfbmFtZSI6IkFkbWluIiwiZW1haWwiOiJhZG1pbnVzZXJAZXhhbXBsZS5jb20ifQ.R9abtB020XMxVwNeco2aUiwB1VjjyIpGdmLEyZXEMz_BAu6bcsohyfkny8I8d8UZ18-Wo2qfgXcZhDwrifGVr3YUnVP_nTJZx5liCM-yYXdnD5CRvSHSLueNk6iiQhE8cHEaJ5Y8oEd0jW0m4ci6XxJd68G5sM_1c3VxLz04mUqZ347n9Cq2B_sXyuN-U95-UAzoHNW23iGy7_Fqe9TCewQEtE7_1xMW3dO8_hCE0O1aUorqkBwWp1FPNQtgQi4bbEHPYI2rJbQXF6yrlagKcmexApiUU93thxulGylX-p64VJP82iw8EOfodiAxWndQk4uuSdVk29uz_AhuOl15fg";
            List<Map.Entry<String, String>> headers = List.of(Map.entry(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON),
                    Map.entry(HEADER_AUTHORIZATION, BEARER + authorizationToken));
            when(getRequest("https://api-conformance.ebsi.eu/conformance/v3/issuer-mock/offers/00067a17-681f-4b41-9794-cb7c98570a7a",headers)).thenReturn(Mono.just("response"));

            when(objectMapper.readValue("response", CredentialOffer.class))
                    .thenThrow(new JsonProcessingException("Deserialization error") {});

            StepVerifier.create(credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId,qrContent))
                    .expectError(FailedDeserializingException.class)
                    .verify();

        }
    }


}
