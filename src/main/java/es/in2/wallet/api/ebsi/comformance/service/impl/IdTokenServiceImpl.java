package es.in2.wallet.api.ebsi.comformance.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import es.in2.wallet.api.ebsi.comformance.service.IdTokenService;
import es.in2.wallet.api.exception.FailedCommunicationException;
import es.in2.wallet.api.exception.ParseErrorException;
import es.in2.wallet.api.model.AuthorisationServerMetadata;
import es.in2.wallet.api.service.SignerService;
import es.in2.wallet.api.util.MessageUtils;
import es.in2.wallet.vault.service.VaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static es.in2.wallet.api.util.MessageUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdTokenServiceImpl implements IdTokenService {
    private final ObjectMapper objectMapper;
    private final VaultService vaultService;
    private final SignerService signerService;
    @Override
    public Mono<Map<String, String>> getIdTokenRequest(String processId, String did, AuthorisationServerMetadata authorisationServerMetadata, String jwt) {
        return  completeTokenExchange(did,authorisationServerMetadata,jwt)
                .doOnSuccess(tokenResponse -> log.info("ProcessID: {} - Token Response: {}", processId, tokenResponse));
    }


    /**
     * Completes the token exchange process using the provided parameters and code verifier.
     */
    private Mono<Map<String, String>> completeTokenExchange(String did, AuthorisationServerMetadata authorisationServerMetadata, String jwt) {
        return buildIdTokenResponse(jwt, authorisationServerMetadata, did)
                .flatMap(MessageUtils::extractAllQueryParams);
    }

    private Mono<String> buildIdTokenResponse(String jwt, AuthorisationServerMetadata authorisationServerMetadata,String did){
        return extractRequiredParamFromJwt(jwt)
                .flatMap(paramsList -> buildSignedJwtForIdToken(paramsList,authorisationServerMetadata,did)
                        .flatMap(idToken -> sendIdTokenResponse(idToken,paramsList)));
    }

    private Mono<String> sendIdTokenResponse(String idToken,List<String> params){
        String body = "id_token=" + URLEncoder.encode(idToken, StandardCharsets.UTF_8)
                + "&state=" + URLEncoder.encode(params.get(1), StandardCharsets.UTF_8);
        List<Map.Entry<String, String>> headers = new ArrayList<>();
        headers.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_URL_ENCODED_FORM));
        String redirectUri = params.get(2);

        return postRequest(redirectUri,headers,body)
                .onErrorResume(e -> Mono.error(new FailedCommunicationException("Error while sending Id Token Response")));
    }
    private Mono<String> buildSignedJwtForIdToken(List<String> params, AuthorisationServerMetadata authorisationServerMetadata,String did){
        Instant issueTime = Instant.now();
        Instant expirationTime = issueTime.plus(10, ChronoUnit.MINUTES);
        JWTClaimsSet payload = new JWTClaimsSet.Builder()
                .issuer(did)
                .subject(did)
                .audience(authorisationServerMetadata.issuer())
                .issueTime(java.util.Date.from(issueTime))
                .expirationTime(java.util.Date.from(expirationTime))
                .claim("nonce", params.get(0))
                .build();
        try {
            JsonNode documentNode = objectMapper.readTree(payload.toString());
            return vaultService.getSecretByKey(did,PRIVATE_KEY_TYPE)
                    .flatMap(privateKey -> signerService.buildJWTSFromJsonNode(documentNode,did,"JWT",privateKey));
        }catch (JsonProcessingException e){
            log.error("Error while parsing the JWT payload", e);
            throw new ParseErrorException("Error while parsing the JWT payload: " + e.getMessage());
        }
    }
    private Mono<List<String>> extractRequiredParamFromJwt(String jwt){
        return Mono.fromCallable(() -> {
            log.debug(jwt);
            SignedJWT signedJwt = SignedJWT.parse(jwt);
            return List.of(signedJwt.getJWTClaimsSet().getClaim("nonce").toString(),
                    signedJwt.getJWTClaimsSet().getClaim("state").toString(),
                    signedJwt.getJWTClaimsSet().getClaim("redirect_uri").toString());
        });

    }
}