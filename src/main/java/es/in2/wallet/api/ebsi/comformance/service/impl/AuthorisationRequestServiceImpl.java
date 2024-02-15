package es.in2.wallet.api.ebsi.comformance.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.api.ebsi.comformance.service.AuthorisationRequestService;
import es.in2.wallet.api.exception.FailedCommunicationException;
import es.in2.wallet.api.model.AuthorisationRequestForIssuance;
import es.in2.wallet.api.model.AuthorisationServerMetadata;
import es.in2.wallet.api.model.CredentialIssuerMetadata;
import es.in2.wallet.api.model.CredentialOffer;
import es.in2.wallet.api.util.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

import static es.in2.wallet.api.util.MessageUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorisationRequestServiceImpl implements AuthorisationRequestService {
    private final ObjectMapper objectMapper;
    @Override
    public Mono<Tuple2<String, String>> getRequestWithOurGeneratedCodeVerifier(String processId, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata, String did) {
        return performAuthorizationFlow(credentialOffer, credentialIssuerMetadata, authorisationServerMetadata, did)
                .doOnSuccess(tokenResponse -> log.info("ProcessID: {} - Token Response: {}", processId, tokenResponse));
    }

    /**
     * Orchestrates the authorization flow.
     * Generates a code verifier, initiates the authorization request and completes the token exchange.
     */
    private Mono<Tuple2<String, String>> performAuthorizationFlow(CredentialOffer credentialOffer, CredentialIssuerMetadata credentialIssuerMetadata, AuthorisationServerMetadata authorisationServerMetadata, String did) {
        return generateCodeVerifier()
                .flatMap(codeVerifier -> initiateAuthorizationRequest(credentialOffer, credentialIssuerMetadata, authorisationServerMetadata, did, codeVerifier))
                .flatMap(this::extractRequest);
    }

    /**
     * Initiates the authorization request by building the auth request, encoding it, sending it,
     * and then extracting all query parameters.
     */
    private Mono<Tuple2<Map<String, String>, String>> initiateAuthorizationRequest(CredentialOffer credentialOffer, CredentialIssuerMetadata credentialIssuerMetadata, AuthorisationServerMetadata authorisationServerMetadata, String did, String codeVerifier) {
        return buildEbsiAuthRequest(credentialOffer, credentialIssuerMetadata, codeVerifier, did)
                .flatMap(this::authRequestBodyToUrlEncodedString)
                .flatMap(authRequestEncodedBody -> sendAuthRequest(authorisationServerMetadata, authRequestEncodedBody))
                .flatMap(MessageUtils::extractAllQueryParams)
                .map(params -> Tuples.of(params, codeVerifier));
    }

    /**
     * Completes the token exchange process using the provided parameters and code verifier.
     */
    private Mono<Tuple2<String, String>> extractRequest( Tuple2<Map<String, String>, String> paramsAndCodeVerifier) {
        Map<String, String> params = paramsAndCodeVerifier.getT1();
        String codeVerifier = paramsAndCodeVerifier.getT2();

        return getJwtRequest(params)
                .map(jwt ->Tuples.of(jwt, codeVerifier));
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
}
