package es.in2.wallet.api.ebsi.comformance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.api.ebsi.comformance.service.impl.IdTokenServiceImpl;
import es.in2.wallet.api.exception.FailedCommunicationException;
import es.in2.wallet.api.exception.ParseErrorException;
import es.in2.wallet.api.model.AuthorisationServerMetadata;
import es.in2.wallet.api.service.SignerService;
import es.in2.wallet.api.util.MessageUtils;
import es.in2.wallet.vault.service.VaultService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import java.util.HashMap;
import java.util.Map;

import static es.in2.wallet.api.util.MessageUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdTokenServiceImplTest {

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private VaultService vaultService;
    @Mock
    private SignerService signerService;

    @InjectMocks
    private IdTokenServiceImpl idTokenService;

    @Test
    void getIdTokenRequest_SuccessfulFlow() throws Exception {
        try (MockedStatic<MessageUtils> ignored = Mockito.mockStatic(MessageUtils.class)) {
            String processId = "processId";
            String did = "did:key:example";
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().issuer("issuer").build();
            String jwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6ImM0S3JlcEpYem1CTVctcW8ybnREQ3drVGdMbTJDYl81ZWFiemtsalRoXzAifQ.eyJpc3MiOiJodHRwczovL2FwaS1jb25mb3JtYW5jZS5lYnNpLmV1L2NvbmZvcm1hbmNlL3YzL2F1dGgtbW9jayIsImF1ZCI6Imh0dHBzOi8vbXktaXNzdWVyLmV1L3N1ZmZpeC94eXoiLCJleHAiOjE1ODk2OTkxNjIsInJlc3BvbnNlX3R5cGUiOiJpZF90b2tlbiIsInJlc3BvbnNlX21vZGUiOiJkaXJlY3RfcG9zdCIsImNsaWVudF9pZCI6Imh0dHBzOi8vYXBpLWNvbmZvcm1hbmNlLmVic2kuZXUvY29uZm9ybWFuY2UvdjMvYXV0aC1tb2NrIiwicmVkaXJlY3RfdXJpIjoiaHR0cHM6Ly9hcGktY29uZm9ybWFuY2UuZWJzaS5ldS9jb25mb3JtYW5jZS92My9hdXRoLW1vY2svZGlyZWN0X3Bvc3QiLCJzY29wZSI6Im9wZW5pZCIsInN0YXRlIjoiNDhhMmJhYzYtMTMwYS00Mzc4LWJjYzItMDRlYjU3YzU0M2I5Iiwibm9uY2UiOiJuLTBTNl9XekEyTWoifQ.d-3D3w99BvRq_N1tmUaDwlaG8oGnOiA4mVs1Cgp1USc1Yhf8TN9G8Vt_SO_LmJGspuqST8ESwUUkmYvXOYj5Pw";

            String expectedIdToken = "idToken";

            String json = "{\"JWT payload\":\"data\"}";
            ObjectMapper objectMapper2 = new ObjectMapper();
            JsonNode jsonNode = objectMapper2.readTree(json);

            when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
            when(vaultService.getSecretByKey(did, PRIVATE_KEY_TYPE)).thenReturn(Mono.just("privateKey"));
            when(signerService.buildJWTSFromJsonNode(jsonNode, did, "JWT", "privateKey")).thenReturn(Mono.just(expectedIdToken));

            when(postRequest(anyString(), anyList(), anyString())).thenReturn(Mono.just("redirect response"));
            Map<String, String> map = new HashMap<>();
            map.put("code","1234");
            map.put("state",GLOBAL_STATE);
            when(extractAllQueryParams("redirect response")).thenReturn(Mono.just(map));

            StepVerifier.create(idTokenService.getIdTokenRequest(processId, did, authorisationServerMetadata, jwt))
                    .assertNext(responseMap -> {
                        assertEquals("1234", responseMap.get("code"));
                        assertEquals(GLOBAL_STATE, responseMap.get("state"));
                    })
                    .verifyComplete();
        }
    }
    @Test
    void getIdTokenRequest_FailureParsingJwt() throws Exception{
        String processId = "processId";
        String did = "did:key:example";
        AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().issuer("issuer").build();
        String jwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6ImM0S3JlcEpYem1CTVctcW8ybnREQ3drVGdMbTJDYl81ZWFiemtsalRoXzAifQ.eyJpc3MiOiJodHRwczovL2FwaS1jb25mb3JtYW5jZS5lYnNpLmV1L2NvbmZvcm1hbmNlL3YzL2F1dGgtbW9jayIsImF1ZCI6Imh0dHBzOi8vbXktaXNzdWVyLmV1L3N1ZmZpeC94eXoiLCJleHAiOjE1ODk2OTkxNjIsInJlc3BvbnNlX3R5cGUiOiJpZF90b2tlbiIsInJlc3BvbnNlX21vZGUiOiJkaXJlY3RfcG9zdCIsImNsaWVudF9pZCI6Imh0dHBzOi8vYXBpLWNvbmZvcm1hbmNlLmVic2kuZXUvY29uZm9ybWFuY2UvdjMvYXV0aC1tb2NrIiwicmVkaXJlY3RfdXJpIjoiaHR0cHM6Ly9hcGktY29uZm9ybWFuY2UuZWJzaS5ldS9jb25mb3JtYW5jZS92My9hdXRoLW1vY2svZGlyZWN0X3Bvc3QiLCJzY29wZSI6Im9wZW5pZCIsInN0YXRlIjoiNDhhMmJhYzYtMTMwYS00Mzc4LWJjYzItMDRlYjU3YzU0M2I5Iiwibm9uY2UiOiJuLTBTNl9XekEyTWoifQ.d-3D3w99BvRq_N1tmUaDwlaG8oGnOiA4mVs1Cgp1USc1Yhf8TN9G8Vt_SO_LmJGspuqST8ESwUUkmYvXOYj5Pw";

        try (MockedStatic<MessageUtils> ignored = Mockito.mockStatic(MessageUtils.class)) {
            when(objectMapper.readTree(anyString())).thenThrow(new JsonProcessingException("Error") {});

            StepVerifier.create(idTokenService.getIdTokenRequest(processId, did, authorisationServerMetadata, jwt))
                    .expectError(ParseErrorException.class)
                    .verify();
        }
    }
    @Test
    void getIdTokenRequest_FailedCommunicationException() throws Exception{
        String processId = "processId";
        String did = "did:key:example";
        AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().issuer("issuer").build();
        String jwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6ImM0S3JlcEpYem1CTVctcW8ybnREQ3drVGdMbTJDYl81ZWFiemtsalRoXzAifQ.eyJpc3MiOiJodHRwczovL2FwaS1jb25mb3JtYW5jZS5lYnNpLmV1L2NvbmZvcm1hbmNlL3YzL2F1dGgtbW9jayIsImF1ZCI6Imh0dHBzOi8vbXktaXNzdWVyLmV1L3N1ZmZpeC94eXoiLCJleHAiOjE1ODk2OTkxNjIsInJlc3BvbnNlX3R5cGUiOiJpZF90b2tlbiIsInJlc3BvbnNlX21vZGUiOiJkaXJlY3RfcG9zdCIsImNsaWVudF9pZCI6Imh0dHBzOi8vYXBpLWNvbmZvcm1hbmNlLmVic2kuZXUvY29uZm9ybWFuY2UvdjMvYXV0aC1tb2NrIiwicmVkaXJlY3RfdXJpIjoiaHR0cHM6Ly9hcGktY29uZm9ybWFuY2UuZWJzaS5ldS9jb25mb3JtYW5jZS92My9hdXRoLW1vY2svZGlyZWN0X3Bvc3QiLCJzY29wZSI6Im9wZW5pZCIsInN0YXRlIjoiNDhhMmJhYzYtMTMwYS00Mzc4LWJjYzItMDRlYjU3YzU0M2I5Iiwibm9uY2UiOiJuLTBTNl9XekEyTWoifQ.d-3D3w99BvRq_N1tmUaDwlaG8oGnOiA4mVs1Cgp1USc1Yhf8TN9G8Vt_SO_LmJGspuqST8ESwUUkmYvXOYj5Pw";


        try (MockedStatic<MessageUtils> ignored = Mockito.mockStatic(MessageUtils.class)) {
            String json = "{\"JWT payload\":\"data\"}";
            JsonNode jsonNode = new ObjectMapper().readTree(json);

            when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
            when(vaultService.getSecretByKey(did, PRIVATE_KEY_TYPE)).thenReturn(Mono.just("privateKey"));
            when(signerService.buildJWTSFromJsonNode(jsonNode, did, "JWT", "privateKey")).thenReturn(Mono.just("signedJwt"));

            when(postRequest(anyString(), anyList(), anyString())).thenReturn(Mono.error(new RuntimeException("Communication failure")));

            StepVerifier.create(idTokenService.getIdTokenRequest(processId, did, authorisationServerMetadata, jwt))
                    .expectError(FailedCommunicationException.class)
                    .verify();
        }
    }


}
