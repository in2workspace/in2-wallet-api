package es.in2.wallet.api.ebsi.comformance.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import es.in2.wallet.api.ebsi.comformance.configuration.EbsiConfig;
import es.in2.wallet.api.ebsi.comformance.service.EbsiAuthorizationCodeService;
import es.in2.wallet.api.exception.FailedCommunicationException;
import es.in2.wallet.api.exception.FailedDeserializingException;
import es.in2.wallet.api.exception.ParseErrorException;
import es.in2.wallet.api.model.*;
import es.in2.wallet.api.service.SignerService;
import es.in2.wallet.vault.service.VaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static es.in2.wallet.api.util.MessageUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EbsiAuthorizationCodeServiceImpl implements EbsiAuthorizationCodeService {
    private final ObjectMapper objectMapper;
    private final EbsiConfig ebsiConfig;
    private final VaultService vaultService;
    private final SignerService signerService;
    @Override
    public Mono<TokenResponse> getTokenResponse(String processId, String authorizationToken, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata) {
        return ebsiConfig.getDid()
                .flatMap(did -> performAuthorizationFlow(credentialOffer, credentialIssuerMetadata, authorisationServerMetadata, did))
                .doOnSuccess(tokenResponse -> log.info("ProcessID: {} - Token Response: {}", processId, tokenResponse));
    }

    /**
     * Orchestrates the authorization flow.
     * Generates a code verifier, initiates the authorization request and completes the token exchange.
     */
    private Mono<TokenResponse> performAuthorizationFlow(CredentialOffer credentialOffer, CredentialIssuerMetadata credentialIssuerMetadata, AuthorisationServerMetadata authorisationServerMetadata, String did) {
        return generateCodeVerifier()
                .flatMap(codeVerifier -> initiateAuthorizationRequest(credentialOffer, credentialIssuerMetadata, authorisationServerMetadata, did, codeVerifier))
                .flatMap(codeVerifier -> completeTokenExchange(authorisationServerMetadata, did, codeVerifier));
    }

    /**
     * Initiates the authorization request by building the auth request, encoding it, sending it,
     * and then extracting all query parameters.
     */
    private Mono<Tuple2<Map<String, String>, String>> initiateAuthorizationRequest(CredentialOffer credentialOffer, CredentialIssuerMetadata credentialIssuerMetadata, AuthorisationServerMetadata authorisationServerMetadata, String did, String codeVerifier) {
        return buildEbsiAuthRequest(credentialOffer, credentialIssuerMetadata, codeVerifier, did)
                .flatMap(this::authRequestBodyToUrlEncodedString)
                .flatMap(authRequestEncodedBody -> sendAuthRequest(authorisationServerMetadata, authRequestEncodedBody))
                .flatMap(this::extractAllQueryParams)
                .map(params -> Tuples.of(params, codeVerifier));
    }

    /**
     * Completes the token exchange process using the provided parameters and code verifier.
     */
    private Mono<TokenResponse> completeTokenExchange(AuthorisationServerMetadata authorisationServerMetadata, String did, Tuple2<Map<String, String>, String> paramsAndCodeVerifier) {
        Map<String, String> params = paramsAndCodeVerifier.getT1();
        String codeVerifier = paramsAndCodeVerifier.getT2();

        return getJwtRequest(params)
                .flatMap(response -> buildIdTokenResponse(response, authorisationServerMetadata, did, params))
                .flatMap(this::extractAllQueryParams)
                .flatMap(codeAndState -> sendTokenRequest(codeVerifier, did, authorisationServerMetadata, codeAndState));
    }
    private Mono<AuthorisationRequestForIssuance> buildEbsiAuthRequest(CredentialOffer credentialOffer, CredentialIssuerMetadata credentialIssuerMetadata, String codeVerifier, String did) {
        return generateEbsiCodeChallenge(codeVerifier)
                .map(codeChallenge -> {
                    List<AuthorisationRequestForIssuance.AuthorizationDetail> authorizationDetailsEbsi =
                            credentialOffer.credentials().stream()
                                    .map(credential -> AuthorisationRequestForIssuance.AuthorizationDetail.builder()
                                            .type("openid_credential")
                                            .locations(List.of(credentialIssuerMetadata.credentialIssuer()))
                                            .format(credential.format())
                                            .types(credential.types())
                                            .build())
                                    .toList();

                    return AuthorisationRequestForIssuance.builder()
                            .responseType("code")
                            .scope("openid")
                            .redirectUri("openid://")
                            .issuerState(credentialOffer.grant().authorizationCodeGrant().issuerState())
                            .clientId(did)
                            .authorizationDetails(authorizationDetailsEbsi)
                            .state(GLOBAL_STATE)
                            .codeChallenge(codeChallenge)
                            .codeChallengeMethod("S256")
                            .build();
                })
                .flatMap(Mono::just);
    }

    private Mono<String> authRequestBodyToUrlEncodedString(AuthorisationRequestForIssuance authRequest) {
        return Mono.fromCallable(() -> {
            JsonNode jsonNode = objectMapper.valueToTree(authRequest);
            StringBuilder result = new StringBuilder();

            for (Iterator<Map.Entry<String, JsonNode>> it = jsonNode.fields(); it.hasNext();) {
                Map.Entry<String, JsonNode> field = it.next();
                JsonNode fieldValue = field.getValue();

                if (fieldValue.isValueNode()) {
                    appendEbsiEncodedField(result, field.getKey(), fieldValue.asText());
                } else if (fieldValue.isArray() && "authorization_details".equals(field.getKey())) {
                    String jsonArray = objectMapper.writeValueAsString(fieldValue);
                    appendEbsiEncodedField(result, field.getKey(), jsonArray);
                } else if (fieldValue.isArray()) {
                    for (JsonNode item : fieldValue) {
                        String itemAsString = item.toString();
                        appendEbsiEncodedField(result, field.getKey(), itemAsString);
                    }
                }
            }
            return result.toString();
        });
    }


    private void appendEbsiEncodedField(StringBuilder result, String key, String value){
        if (result.length() > 0) result.append("&");
        result.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                .append("=")
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    private Mono<String> sendAuthRequest(AuthorisationServerMetadata authorisationServerMetadata, String authRequestEncodedBody){
        List<Map.Entry<String, String>> headers = new ArrayList<>();
        String urlWithParams = authorisationServerMetadata.authorizationEndpoint() + "?" + authRequestEncodedBody;
        return getRequest(urlWithParams, headers)
                .onErrorResume(e -> Mono.error(new FailedCommunicationException("Error while sending Authorization Request")));
    }

    private Mono<String> getJwtRequest(Map<String, String> params) {
        if (params.get("request_uri") != null){
            List<Map.Entry<String, String>> headers = new ArrayList<>();
            String requestUri = params.get("request_uri");
            return getRequest(requestUri, headers);
        }
        else if (params.get("request") != null){
            return Mono.just(params.get("request"));
        }
        else {return Mono.error(new IllegalArgumentException("theres any request found in parameters"));}
    }
    private Mono<String> buildIdTokenResponse(String jwt, AuthorisationServerMetadata authorisationServerMetadata,String did,Map<String, String> params){
        return extractNonceAndStateFromJwt(jwt)
                .flatMap(paramsList -> buildSignedJwtForIdToken(paramsList,authorisationServerMetadata,did)
                        .flatMap(idToken -> sendIdTokenResponse(idToken,params,paramsList)));
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
    private Mono<String> sendIdTokenResponse(String idToken,Map<String, String> params,List<String> state){
        String body = "id_token=" + URLEncoder.encode(idToken, StandardCharsets.UTF_8)
                + "&state=" + URLEncoder.encode(state.get(1), StandardCharsets.UTF_8);
        List<Map.Entry<String, String>> headers = new ArrayList<>();
        headers.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_URL_ENCODED_FORM));
        String redirectUri = params.get("redirect_uri");

        return postRequest(redirectUri,headers,body)
                .onErrorResume(e -> Mono.error(new FailedCommunicationException("Error while sending Id Token Response")));
    }
    private Mono<String> buildSignedJwtForIdToken(List<String> nonceAndState, AuthorisationServerMetadata authorisationServerMetadata,String did){
        Instant issueTime = Instant.now();
        Instant expirationTime = issueTime.plus(10, ChronoUnit.MINUTES);
        JWTClaimsSet payload = new JWTClaimsSet.Builder()
                .issuer(did)
                .subject(did)
                .audience(authorisationServerMetadata.issuer())
                .issueTime(java.util.Date.from(issueTime))
                .expirationTime(java.util.Date.from(expirationTime))
                .claim("nonce", nonceAndState.get(0))
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
    private Mono<List<String>> extractNonceAndStateFromJwt(String jwt){
        return Mono.fromCallable(() -> {
            log.debug(jwt);
            SignedJWT signedJwt = SignedJWT.parse(jwt);
            return List.of(signedJwt.getJWTClaimsSet().getClaim("nonce").toString(),signedJwt.getJWTClaimsSet().getClaim("state").toString());
        });

    }
    /**
     * Generates a cryptographically strong and random code verifier.
     * The length is random, ranging between 43 and 128 characters.
     * Only uses unreserved characters [A-Z], [a-z], [0-9], "-", ".", "_", "~".
     *
     * @return A random code verifier string.
     */
    private Mono<String> generateCodeVerifier(){
        return Mono.fromCallable(() -> {
            int length = SecureRandom.getInstanceStrong().nextInt(86) + 43; // Random length between 43 and 128
            StringBuilder sb = new StringBuilder(length);

            for (int i = 0; i < length; i++) {
                sb.append(CODEVERIFIERALLOWEDCHARACTERS.charAt(SecureRandom.getInstanceStrong().nextInt(CODEVERIFIERALLOWEDCHARACTERS.length())));
            }
            return sb.toString();
        });
    }
    /**
     * Creates a code challenge from the provided code verifier.
     * Applies SHA256 hash and then encodes it using BASE64URL.
     *
     * @param codeVerifier The code verifier string.
     * @return A BASE64URL-encoded SHA256 hash of the code verifier.
     */
    private Mono<String> generateEbsiCodeChallenge(String codeVerifier){
        return Mono.fromCallable(() -> {
            // Apply SHA-256 hash to the code verifier
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes());

            // Encode the hash using BASE64URL without padding
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        });
    }

    private Mono<Map<String, String>> extractAllQueryParams(String url) {
        log.debug(url);
        return Mono.fromCallable(() -> {
            Map<String, String> params = new HashMap<>();
            try {
                URI uri = new URI(url);
                String query = uri.getQuery();
                if (query != null) {
                    String[] pairs = query.split("&");
                    for (String pair : pairs) {
                        int idx = pair.indexOf("=");
                        String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                        String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                        params.put(key, value);
                    }
                }
            } catch (Exception e) {
                log.debug(e.getMessage());
            }
            return params;
        });
    }
}