package es.in2.wallet.domain.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.domain.exception.FailedCommunicationException;
import es.in2.wallet.domain.exception.FailedDeserializingException;
import es.in2.wallet.domain.model.*;
import es.in2.wallet.domain.service.EbsiAuthorisationService;
import es.in2.wallet.domain.util.ApplicationUtils;
import es.in2.wallet.infrastructure.core.config.WebClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

import static es.in2.wallet.domain.util.ApplicationConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EbsiAuthorisationServiceImpl implements EbsiAuthorisationService {

    private final ObjectMapper objectMapper;
    private final WebClientConfig webClient;

    @Override
    public Mono<Tuple2<String, String>> getRequestWithOurGeneratedCodeVerifier(String processId, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata, String did) {
        return performAuthorizationRequest(credentialOffer, credentialIssuerMetadata, authorisationServerMetadata, did).doOnSuccess(tokenResponse -> log.info("ProcessID: {} - Token Response: {}", processId, tokenResponse));
    }

    /**
     * Orchestrates the authorization request flow.
     * Generates a code verifier, initiates the authorization request.
     */
    private Mono<Tuple2<String, String>> performAuthorizationRequest(CredentialOffer credentialOffer, CredentialIssuerMetadata credentialIssuerMetadata, AuthorisationServerMetadata authorisationServerMetadata, String did) {
        return generateCodeVerifier()
                .flatMap(codeVerifier -> initiateAuthorizationRequest(credentialOffer, credentialIssuerMetadata, authorisationServerMetadata, did, codeVerifier))
                .flatMap(this::extractRequest);
    }

    /**
     * Initiates the authorization request by building the auth request, encoding it, sending it,
     * and then extracting all query parameters.
     */
    private Mono<Tuple2<Map<String, String>, String>> initiateAuthorizationRequest(CredentialOffer credentialOffer, CredentialIssuerMetadata credentialIssuerMetadata, AuthorisationServerMetadata authorisationServerMetadata, String did, String codeVerifier) {
        return buildAuthRequest(credentialOffer, credentialIssuerMetadata, codeVerifier, did)
                .flatMap(this::authRequestBodyToUrlEncodedString)
                .flatMap(authRequestEncodedBody -> sendAuthRequest(authorisationServerMetadata, authRequestEncodedBody))
                .flatMap(ApplicationUtils::extractAllQueryParams).map(params -> Tuples.of(params, codeVerifier));
    }

    /**
     * Extracts the JWT and the code verifier from the given parameters and code verifier tuple.
     * This method uses the provided parameters to retrieve a JWT using the {@link #getJwtRequest} method.
     * It then combines the retrieved JWT with the code verifier into a Tuple2 object.
     *
     * @param paramsAndCodeVerifier A Tuple2 containing a map of parameters and a code verifier string.
     *                              The parameter map is expected to include either 'request' or 'request_uri' used to retrieve the JWT.
     *                              The code verifier is a string used in the OAuth 2.0 PKCE flow.
     */
    private Mono<Tuple2<String, String>> extractRequest(Tuple2<Map<String, String>, String> paramsAndCodeVerifier) {
        Map<String, String> params = paramsAndCodeVerifier.getT1();
        String codeVerifier = paramsAndCodeVerifier.getT2();

        return getJwtRequest(params).map(jwt -> Tuples.of(jwt, codeVerifier));
    }

    /**
     * Builds the authorisation request for issuance of Verifiable Credentials (VCs) based on the OAuth 2.0 Rich Authorisation Request framework.
     * This method constructs the request by specifying the types of VCs being requested using the {@code authorization_details} parameter.
     * It leverages Proof Key for Code Exchange (PKCE) by generating a code challenge from the provided code verifier.
     *
     * @param credentialOffer          The credential offer received, which includes details about the VCs being offered.
     * @param credentialIssuerMetadata Metadata about the credential issuer, including the issuer's URL.
     * @param codeVerifier             The code verifier for PKCE to secure the code exchange process in the OAuth 2.0 flow.
     * @param did                      The decentralized identifier (DID) representing the client's identity.
     *                                 This request is initiated by the Client (Holder Wallet) to the Authorisation Server to request the issuance of specific VCs.
     *                                 The request must be compatible with a Request Object signed by the Relying Party, and the Client should only use PKCE for security.
     */
    private Mono<AuthorisationRequestForIssuance> buildAuthRequest(CredentialOffer credentialOffer, CredentialIssuerMetadata credentialIssuerMetadata, String codeVerifier, String did) {
        return generatePKCECodeChallenge(codeVerifier).map(codeChallenge -> {
            List<AuthorisationRequestForIssuance.AuthorizationDetail> authorizationDetailsEbsi = credentialOffer.credentials().stream().map(credential -> AuthorisationRequestForIssuance.AuthorizationDetail.builder().type("openid_credential").locations(List.of(credentialIssuerMetadata.credentialIssuer())).format(credential.format()).types(credential.types()).build()).toList();
            return AuthorisationRequestForIssuance.builder().responseType("code").scope("openid").redirectUri("openid://").issuerState(credentialOffer.grant().authorizationCodeGrant().issuerState()).clientId(did).authorizationDetails(authorizationDetailsEbsi).state(GLOBAL_STATE).codeChallenge(codeChallenge).codeChallengeMethod("S256").build();
        }).flatMap(Mono::just);
    }

    private Mono<String> authRequestBodyToUrlEncodedString(AuthorisationRequestForIssuance authRequest) {
        return Mono.fromCallable(() -> {
            JsonNode jsonNode = objectMapper.valueToTree(authRequest);
            StringBuilder result = new StringBuilder();
            for (Iterator<Map.Entry<String, JsonNode>> it = jsonNode.fields(); it.hasNext(); ) {
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

    private void appendEbsiEncodedField(StringBuilder result, String key, String value) {
        // todo: check if this is the correct encoding - old code: result.length() > 0
        if (!result.isEmpty()) {
            result.append("&");
        }
        result.append(URLEncoder.encode(key, StandardCharsets.UTF_8)).append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    private Mono<String> sendAuthRequest(AuthorisationServerMetadata authorisationServerMetadata, String authRequestEncodedBody) {
        String urlWithParams = authorisationServerMetadata.authorizationEndpoint() + "?" + authRequestEncodedBody;
        return webClient.centralizedWebClient()
                .get()
                .uri(urlWithParams)
                .exchangeToMono(response -> {
                    if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                        return Mono.error(new RuntimeException("There was an error during the Authorization request, error" + response));
                    }
                    else {
                        log.info("EBSI authorization request response: {}", response);
                        return Mono.just(Objects.requireNonNull(response.headers().asHttpHeaders().getFirst(HttpHeaders.LOCATION)));
                    }
                })
                .onErrorResume(e -> Mono.error(new FailedCommunicationException("Error while sending Authorization Request")));
    }

    /**
     * Extracts and returns a JWT based on the provided parameters.
     * The method checks for either a 'request' or 'request_uri' parameter in the provided map.
     * If 'request_uri' is present, it performs a GET request to the specified URI to retrieve the JWT.
     * If 'request' is present, it directly returns the JWT contained within the parameter value.
     *
     * @param params A map containing the request parameters, expected to include either 'request' or 'request_uri'.
     * @throws IllegalArgumentException if neither 'request' nor 'request_uri' is found in the parameters.
     */
    private Mono<String> getJwtRequest(Map<String, String> params) {
        if (params.get("request_uri") != null) {
            String requestUri = params.get("request_uri");
            return webClient.centralizedWebClient()
                    .get()
                    .uri(requestUri)
                    .exchangeToMono(response -> {
                        if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                            return Mono.error(new RuntimeException("There was an error retrieving EBSI authorisation request, error" + response));
                        }
                        else {
                            log.info("EBSI authorization request: {}", response);
                            return response.bodyToMono(String.class);
                        }
                    });
        } else if (params.get("request") != null) {
            return Mono.just(params.get("request"));
        } else {
            return Mono.error(new IllegalArgumentException("theres any request found in parameters"));
        }
    }

    /**
     * Generates a cryptographically strong and random code verifier.
     * The length is random, ranging between 43 and 128 characters.
     * Only uses unreserved characters [A-Z], [a-z], [0-9], "-", ".", "_", "~".
     *
     * @return A random code verifier string.
     */
    private Mono<String> generateCodeVerifier() {
        return Mono.fromCallable(() -> {
            int length = SecureRandom.getInstanceStrong().nextInt(86) + 43; // Random length between 43 and 128
            StringBuilder sb = new StringBuilder(length);

            for (int i = 0; i < length; i++) {
                sb.append(CODE_VERIFIER_ALLOWED_CHARACTERS.charAt(SecureRandom.getInstanceStrong().nextInt(CODE_VERIFIER_ALLOWED_CHARACTERS.length())));
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
    private Mono<String> generatePKCECodeChallenge(String codeVerifier) {
        return Mono.fromCallable(() -> {
            // Apply SHA-256 hash to the code verifier
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes());

            // Encode the hash using BASE64URL without padding
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        });
    }

    /**
     * Sends a token request to the Authorisation Server's token endpoint using the provided parameters.
     * This method is part of the code flow in OAuth 2.0, specifically designed to secure the exchange
     * of the authorisation code for a token. It uses PKCE to enhance security by verifying the code
     * verifier against the initially provided code challenge.
     * <p>
     * It also checks for a state match to ensure the request response cycle is not intercepted.
     * Upon success, it deserializes the response into a {@link TokenResponse} object.
     *
     * @param codeVerifier                The code verifier for the PKCE, which the Authorisation Server will use
     *                                    to hash and compare against the previously received code challenge.
     * @param did                         The decentralized identifier representing the client's identity.
     * @param authorisationServerMetadata Metadata containing the token endpoint URL of the
     *                                    Authorisation Server.
     * @param params                      A map of parameters received from the Authorisation Server, including the
     *                                    authorisation code and the state to be verified.
     */
    @Override
    public Mono<TokenResponse> sendTokenRequest(String codeVerifier, String did, AuthorisationServerMetadata authorisationServerMetadata, Map<String, String> params) {
        if (Objects.equals(params.get("state"), GLOBAL_STATE)) {
            String code = params.get("code");
            // Build URL encoded form data request body
            Map<String, String> formDataMap = Map.of("grant_type", AUTH_CODE_GRANT_TYPE, "client_id", did, "code", code, "code_verifier", codeVerifier);
            String xWwwFormUrlencodedBody = formDataMap.entrySet().stream().map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
            return webClient.centralizedWebClient()
                    .post()
                    .uri(authorisationServerMetadata.tokenEndpoint())
                    .header(CONTENT_TYPE, CONTENT_TYPE_URL_ENCODED_FORM)
                    .bodyValue(xWwwFormUrlencodedBody)
                    .exchangeToMono(response -> {
                        if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                            return Mono.error(new RuntimeException("There was an error during the token request, error" + response));
                        } else {
                            log.info("Token Response: {}", response);
                            return response.bodyToMono(String.class);
                        }
                    }).flatMap(response -> {
                try {
                    TokenResponse tokenResponse = objectMapper.readValue(response, TokenResponse.class);
                    return Mono.just(tokenResponse);
                } catch (Exception e) {
                    log.error("Error while deserializing TokenResponse from the auth server", e);
                    return Mono.error(new FailedDeserializingException("Error while deserializing TokenResponse: " + response));
                }
            }).onErrorResume(e -> Mono.error(new FailedCommunicationException("Error while sending Token Request")));
        } else {
            return Mono.error(new IllegalArgumentException("state mismatch"));
        }
    }

}
