package es.in2.wallet.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import es.in2.wallet.domain.exception.ParseErrorException;
import es.in2.wallet.domain.service.impl.ProofJWTServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProofJWTServiceImplTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ProofJWTServiceImpl proofJWTService;

    @Test
    void buildCredentialRequestTest() throws JsonProcessingException {
        String nonce = "sampleNonce";
        String issuer = "sampleIssuer";
        String did = "sampleDid";

        // Simulating the JWTClaimsSet payload
        JWTClaimsSet payload = new JWTClaimsSet.Builder()
                .issuer(did)
                .audience(issuer)
                .claim("nonce", nonce)
                .build();

        // Mocking the readTree call to return a JsonNode based on the payload
        JsonNode mockJsonNode = new ObjectMapper().readTree(payload.toString());
        when(objectMapper.readTree(anyString())).thenReturn(mockJsonNode);

        // Executing the test
        StepVerifier.create(proofJWTService.buildCredentialRequest(nonce, issuer, did))
                .expectNextMatches(jsonNode ->
                        jsonNode.has("iss") && jsonNode.get("iss").asText().equals(did) &&
                                jsonNode.has("aud") && jsonNode.get("aud").asText().equals(issuer) &&
                                jsonNode.has("nonce") && jsonNode.get("nonce").asText().equals(nonce)
                )
                .verifyComplete();
    }

    @Test
    void buildCredentialRequestTestParseErrorException() throws JsonProcessingException {
        String nonce = "sampleNonce";
        String issuer = "sampleIssuer";
        String did = "sampleDid";
        when(objectMapper.readTree(anyString())).thenThrow(new JsonProcessingException("Serialization error") {});

        // Executing the test
        StepVerifier.create(proofJWTService.buildCredentialRequest(nonce, issuer, did))
                .expectError(ParseErrorException.class)
                .verify();
    }
}
