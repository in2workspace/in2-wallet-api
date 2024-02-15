package es.in2.wallet.api.ebsi.comformance.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import es.in2.wallet.api.ebsi.comformance.service.IdTokenService;
import es.in2.wallet.api.exception.FailedCommunicationException;
import es.in2.wallet.api.exception.FailedDeserializingException;
import es.in2.wallet.api.exception.ParseErrorException;
import es.in2.wallet.api.model.AuthorisationServerMetadata;
import es.in2.wallet.api.model.TokenResponse;
import es.in2.wallet.api.service.SignerService;
import es.in2.wallet.api.util.MessageUtils;
import es.in2.wallet.vault.service.VaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static es.in2.wallet.api.util.MessageUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdTokenServiceImpl implements IdTokenService {
    private final ObjectMapper objectMapper;
    private final VaultService vaultService;
    private final SignerService signerService;
    @Override
    public Mono<TokenResponse> getTokenResponse(String processId, AuthorisationServerMetadata authorisationServerMetadata, String did, Tuple2<String, String> jwtAndCodeVerifier) {
        return  completeTokenExchange(authorisationServerMetadata,did,jwtAndCodeVerifier)
                .doOnSuccess(tokenResponse -> log.info("ProcessID: {} - Token Response: {}", processId, tokenResponse));
    }


    /**
     * Completes the token exchange process using the provided parameters and code verifier.
     */
    private Mono<TokenResponse> completeTokenExchange(AuthorisationServerMetadata authorisationServerMetadata, String did, Tuple2<String, String> jwtAndCodeVerifier) {
        String jwt = jwtAndCodeVerifier.getT1();
        String codeVerifier = jwtAndCodeVerifier.getT2();

        return buildIdTokenResponse(jwt, authorisationServerMetadata, did)
                .flatMap(MessageUtils::extractAllQueryParams)
                .flatMap(codeAndState -> sendTokenRequest(codeVerifier, did, authorisationServerMetadata, codeAndState));
    }

    private Mono<String> buildIdTokenResponse(String jwt, AuthorisationServerMetadata authorisationServerMetadata,String did){
        return extractRequiredParamFromJwt(jwt)
                .flatMap(paramsList -> buildSignedJwtForIdToken(paramsList,authorisationServerMetadata,did)
                        .flatMap(idToken -> sendIdTokenResponse(idToken,paramsList)));
    }

    private Mono<TokenResponse> sendTokenRequest(String codeVerifier, String did, AuthorisationServerMetadata authorisationServerMetadata,Map<String, String> params){
        if(Objects.equals(params.get("state"), GLOBAL_STATE)){
            String code = params.get("code");
            List<Map.Entry<String, String>> headers = new ArrayList<>();
            headers.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_URL_ENCODED_FORM));
            // Build URL encoded form data request body
            Map<String, String> formDataMap = Map.of("grant_type", AUTH_CODE_GRANT_TYPE, "client_id", did, "code" ,code, "code_verifier",codeVerifier);
            String xWwwFormUrlencodedBody = formDataMap.entrySet().stream()
                    .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
            return postRequest(authorisationServerMetadata.tokenEndpoint(),headers,xWwwFormUrlencodedBody)
                    .flatMap(response ->{
                        try {
                            TokenResponse tokenResponse = objectMapper.readValue(response, TokenResponse.class);
                            return Mono.just(tokenResponse);
                        } catch (Exception e) {
                            log.error("Error while deserializing TokenResponse from the auth server", e);
                            return Mono.error(new FailedDeserializingException("Error while deserializing TokenResponse: " + response));
                        }

                    })
                    .onErrorResume(e -> Mono.error(new FailedCommunicationException("Error while sending Token Request")));
        }
        else {
            return Mono.error(new IllegalArgumentException("state mismatch"));
        }
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