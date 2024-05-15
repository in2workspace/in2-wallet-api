package es.in2.wallet.domain.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import es.in2.wallet.domain.exception.ParseErrorException;
import es.in2.wallet.domain.model.AuthorisationServerMetadata;
import es.in2.wallet.domain.service.EbsiIdTokenService;
import es.in2.wallet.domain.service.SignerService;
import es.in2.wallet.domain.util.ApplicationUtils;
import es.in2.wallet.infrastructure.core.config.WebClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static es.in2.wallet.domain.util.ApplicationConstants.CONTENT_TYPE;
import static es.in2.wallet.domain.util.ApplicationConstants.CONTENT_TYPE_URL_ENCODED_FORM;

@Slf4j
@Service
@RequiredArgsConstructor
public class EbsiIdTokenServiceImpl implements EbsiIdTokenService {
    private final ObjectMapper objectMapper;
    private final SignerService signerService;
    private final WebClientConfig webClient;

    /**
     * Initiates the ID Token Request process by completing the id token exchange with the Authorisation Server.
     * This process involves building a signed JWT ID Token and sending it to the specified redirect URI.
     * The method logs the id token response upon successful exchange.
     *
     * @param processId An identifier for the current process for logging purposes.
     * @param did The decentralized identifier (DID) representing the client's identity.
     * @param authorisationServerMetadata Metadata related to the Authorisation Server.
     * @param jwt The JWT obtained in the previous steps of the OAuth 2.0 flow.
     */
    @Override
    public Mono<Map<String, String>> getIdTokenResponse(String processId, String did, AuthorisationServerMetadata authorisationServerMetadata, String jwt) {
        return  completeIdTokenExchange(did,authorisationServerMetadata,jwt)
                .doOnSuccess(tokenResponse -> log.info("ProcessID: {} - Token Response: {}", processId, tokenResponse));
    }

    /**
     * Completes the id token exchange process using the DID, authorisation server metadata, and the JWT.
     * This involves building a response for the ID Token and extracting all query parameters from it.
     *
     * @param did The decentralized identifier (DID) of the client.
     * @param authorisationServerMetadata Metadata of the Authorisation Server.
     * @param jwt The JWT provided for token exchange.
     */
    private Mono<Map<String, String>> completeIdTokenExchange(String did, AuthorisationServerMetadata authorisationServerMetadata, String jwt) {
        return buildIdTokenResponse(jwt, authorisationServerMetadata, did)
                .flatMap(ApplicationUtils::extractAllQueryParams);
    }

    /**
     * Builds the ID Token response, signed with the client's DID-controlled keys.
     * The response includes required claims such as issuer, subject, audience, and nonce.
     * It also includes the state parameter in the ID Token Request for security.
     *
     * @param jwt The JWT to extract parameters from and to build the ID Token response.
     * @param authorisationServerMetadata Metadata of the Authorisation Server.
     * @param did The DID of the client.
     */
    private Mono<String> buildIdTokenResponse(String jwt, AuthorisationServerMetadata authorisationServerMetadata,String did){
        return extractRequiredParamFromJwt(jwt)
                .flatMap(paramsList -> buildSignedJwtForIdToken(paramsList,authorisationServerMetadata,did)
                        .flatMap(idToken -> sendIdTokenResponse(idToken,paramsList)));
    }

    /**
     * Sends the ID Token response to the specified redirect URI.
     * The response is URL-encoded and includes the ID Token and state parameters.
     *
     * @param idToken The signed ID Token to be sent.
     * @param params The list of parameters extracted from the JWT, including the nonce and state.
     */
    private Mono<String> sendIdTokenResponse(String idToken,List<String> params){
        String body = "id_token=" + URLEncoder.encode(idToken, StandardCharsets.UTF_8)
                + "&state=" + URLEncoder.encode(params.get(1), StandardCharsets.UTF_8);
        String redirectUri = params.get(2);

        return webClient.centralizedWebClient()
                .post()
                .uri(redirectUri)
                .header(CONTENT_TYPE, CONTENT_TYPE_URL_ENCODED_FORM)
                .bodyValue(body)
                .exchangeToMono(response -> {
                    if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                        return Mono.error(new RuntimeException("There was an error during the ID token response, error" + response));
                    } else {
                        return Mono.just(Objects.requireNonNull(response.headers().asHttpHeaders().getFirst(HttpHeaders.LOCATION)));
                    }
                });
    }

    /**
     * Builds a signed JWT for the ID Token using the provided parameters and the DID-controlled keys.
     * The JWT includes standard claims such as issue time and expiration time, along with the nonce.
     *
     * @param params The list of parameters extracted from the initial JWT.
     * @param authorisationServerMetadata Metadata of the Authorisation Server.
     * @param did The DID of the client.
     */
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
            return signerService.buildJWTSFromJsonNode(documentNode,did,"JWT");
        }catch (JsonProcessingException e){
            log.error("Error while parsing the JWT payload", e);
            throw new ParseErrorException("Error while parsing the JWT payload: " + e.getMessage());
        }
    }

    /**
     * Extracts required parameters from the provided JWT.
     * These parameters include the nonce, state, and redirect_uri, which are essential for building the ID Token response.
     *
     * @param jwt The JWT from which to extract the parameters.
     */
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
