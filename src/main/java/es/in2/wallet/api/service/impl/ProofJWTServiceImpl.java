package es.in2.wallet.api.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import es.in2.wallet.api.exception.ParseErrorException;
import es.in2.wallet.api.service.ProofJWTService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProofJWTServiceImpl implements ProofJWTService {
    private final ObjectMapper objectMapper;
    @Override
    public Mono<JsonNode> buildCredentialRequest(String nonce, String issuer) {
        try {
            Instant issueTime = Instant.now();
            JWTClaimsSet payload = new JWTClaimsSet.Builder()
                    .audience(issuer)
                    .issueTime(java.util.Date.from(issueTime))
                    .claim("nonce", nonce)
                    .build();
            return Mono.just(objectMapper.readTree(payload.toString()));
        }
        catch (JsonProcessingException e){
            log.error("Error while parsing the JWT payload", e);
            throw new ParseErrorException("Error while parsing the JWT payload: " + e.getMessage());
        }
    }
}
