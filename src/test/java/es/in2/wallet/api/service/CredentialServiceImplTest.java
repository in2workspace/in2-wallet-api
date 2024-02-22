//package es.in2.wallet.api.service;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.ObjectWriter;
//import es.in2.wallet.api.exception.FailedCommunicationException;
//import es.in2.wallet.api.exception.FailedDeserializingException;
//import es.in2.wallet.api.exception.FailedSerializingException;
//import es.in2.wallet.api.exception.ParseErrorException;
//import es.in2.wallet.api.model.CredentialIssuerMetadata;
//import es.in2.wallet.api.model.CredentialResponse;
//import es.in2.wallet.api.model.TokenResponse;
//import es.in2.wallet.api.service.impl.CredentialServiceImpl;
//import es.in2.wallet.api.util.ApplicationUtils;
//import es.in2.wallet.api.util.MessageUtils;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockedStatic;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpHeaders;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
//import java.util.AbstractMap;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//import static es.in2.wallet.api.util.ApplicationUtils.postRequest;
//import static es.in2.wallet.api.util.MessageUtils.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class CredentialServiceImplTest {
//    @Mock
//    private ObjectMapper objectMapper;
//    @InjectMocks
//    private CredentialServiceImpl credentialService;
//
//    @Test
//    void getCredentialTest() throws JsonProcessingException {
//        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
//            String processId = "processId";
//
//            String jwt = "ey34324";
//
//            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();
//
//            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();
//
//
//            CredentialResponse mockCredentialResponse = CredentialResponse.builder().credential("credential").c_nonce("fresh_nonce").c_nonce_expires_in(600).format("jwt").build();
//
//
//            List<Map.Entry<String, String>> headersForIssuer = new ArrayList<>();
//            headersForIssuer.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));
//            headersForIssuer.add(new AbstractMap.SimpleEntry<>(HEADER_AUTHORIZATION, BEARER + tokenResponse.accessToken()));
//
//
//            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");
//            when(objectMapper.readValue(anyString(), eq(CredentialResponse.class))).thenReturn(mockCredentialResponse);
//
//            when(postRequest(credentialIssuerMetadata.credentialEndpoint(), headersForIssuer, "credentialRequest")).thenReturn(Mono.just("credential"));
//
//            StepVerifier.create(credentialService.getCredential(processId, jwt,tokenResponse, credentialIssuerMetadata))
//                    .expectNext(mockCredentialResponse)
//                    .verifyComplete();
//        }
//    }
//    @Test
//    void getCredentialFailedCommunicationErrorTest() throws JsonProcessingException{
//        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
//            String processId = "processId";
//
//            String jwt = "ey34324";
//
//            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();
//
//            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();
//
//            List<Map.Entry<String, String>> headersForIssuer = new ArrayList<>();
//            headersForIssuer.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));
//            headersForIssuer.add(new AbstractMap.SimpleEntry<>(HEADER_AUTHORIZATION, BEARER + tokenResponse.accessToken()));
//
//            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");
//
//            when(postRequest(credentialIssuerMetadata.credentialEndpoint(), headersForIssuer, "credentialRequest"))
//                    .thenReturn(Mono.error(new RuntimeException("Communication error")));
//
//            StepVerifier.create(credentialService.getCredential(processId, jwt,tokenResponse, credentialIssuerMetadata))
//                    .expectError(FailedCommunicationException.class)
//                    .verify();
//        }
//    }
//    @Test
//    void getCredentialFailedDeserializingErrorTest() throws JsonProcessingException{
//        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
//
//            String processId = "processId";
//
//            String jwt = "ey34324";
//
//            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();
//
//            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();
//
//            List<Map.Entry<String, String>> headersForIssuer = new ArrayList<>();
//            headersForIssuer.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));
//            headersForIssuer.add(new AbstractMap.SimpleEntry<>(HEADER_AUTHORIZATION, BEARER + tokenResponse.accessToken()));
//
//
//            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");
//            when(objectMapper.readValue(anyString(), eq(CredentialResponse.class)))
//                    .thenThrow(new JsonProcessingException("Deserialization error") {});
//
//            when(postRequest(credentialIssuerMetadata.credentialEndpoint(), headersForIssuer, "credentialRequest")).thenReturn(Mono.just("credential"));
//
//            StepVerifier.create(credentialService.getCredential(processId, jwt,tokenResponse, credentialIssuerMetadata))
//                    .expectError(FailedDeserializingException.class)
//                    .verify();
//        }
//    }
//
//    @Test
//    void getCredentialFailedSerializingExceptionTest() throws JsonProcessingException {
//        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
//            String processId = "processId";
//
//            String jwt = "ey34324";
//
//            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();
//
//            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();
//
//            when(objectMapper.writeValueAsString(any()))
//                    .thenThrow(new JsonProcessingException("Serialization error") {});
//
//            StepVerifier.create(credentialService.getCredential(processId, jwt,tokenResponse, credentialIssuerMetadata))
//                    .expectError(FailedSerializingException.class)
//                    .verify();
//        }
//    }
//
//}
