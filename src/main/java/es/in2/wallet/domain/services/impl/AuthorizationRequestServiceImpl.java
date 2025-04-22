package es.in2.wallet.domain.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSObject;
import es.in2.wallet.application.dto.AuthorizationRequestOIDC4VP;
import es.in2.wallet.domain.services.AuthorizationRequestService;
import es.in2.wallet.infrastructure.appconfiguration.exception.MissingAuthorizationRequestParameterException;
import es.in2.wallet.infrastructure.core.config.WebClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static es.in2.wallet.domain.utils.ApplicationConstants.SCOPE_CLAIM;
import static es.in2.wallet.domain.utils.ApplicationUtils.extractAllQueryParams;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationRequestServiceImpl implements AuthorizationRequestService {
    private final WebClientConfig webClient;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<String> getJwtRequestObjectFromUri(String processId, String qrContent) {
        //log.info("Processing a auth request object");
        log.info("ProcessID: {} - Starting to process the Auth Request Object", processId);
        log.info("ProcessID: {} - QR content received: {}", processId, qrContent);
        // Get Authorization Request executing the VC Login Request
        return extractAllQueryParams(qrContent)
                .doOnSuccess(params -> log.info("ProcessID: {} - Extracted query params: {}", processId, params))
                .flatMap(this::getJwtRequestObject)
                .doOnSuccess(response -> log.info("ProcessID: {} - Request Response: {}", processId, response))
                .onErrorResume(e -> {
                    System.out.println("Hola 6");
                    log.error("ProcessID: {} - Error while processing Request Object from the Issuer: {}", processId, e.getMessage());
                    return Mono.error(new RuntimeException("Error while processing Request Object from the Issuer"));
                });
    }

    private Mono<String> getJwtRequestObject(Map<String, String> params) {
        String requestUri = params.get("request_uri");
        String requestInline = params.get("request");

        if (requestUri != null) {
            log.info("Trying to fetch JWT Authorization Request from request_uri: {}", requestUri);
            return webClient.centralizedWebClient()
                    .get()
                    .uri(requestUri)
                    .exchangeToMono(response -> {
                        log.info("Received response with status: {}", response.statusCode());
                        System.out.println("Hola 1");
                        if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                            //return Mono.error(new RuntimeException("There was an error retrieving the authorisation request, error" + response));
                            return response.bodyToMono(String.class)
                                    .defaultIfEmpty("<empty body>")
                                    .flatMap(errorBody -> {
                                        System.out.println("Hola 2");
                                        log.error("Error response body: {}", errorBody);
                                        return Mono.error(new RuntimeException("Error retrieving the authorization request. Status: " + response.statusCode()));
                                    });
                        }
                        else {
//                            log.info("Authorization request: {}", response);
//                            return response.bodyToMono(String.class);
                            return response.bodyToMono(String.class)
                                    .doOnNext(body -> {
                                        System.out.println("Hola 3");
                                        log.info("Fetched JWT Authorization Request: {}", body);
                                    });
                        }
                    });
        } else if (requestInline != null) {
            System.out.println("Hola 4");
            log.info("JWT Authorization Request is provided inline via 'request' parameter.");
            return Mono.just(requestInline);
        } else {
            System.out.println("Hola 5");
            log.info("No 'request' or 'request_uri' parameters were found in the QR content.");
            return Mono.error(new MissingAuthorizationRequestParameterException("Expected 'request' or 'request_uri' in parameters"));
        }
    }

    @Override
    public Mono<AuthorizationRequestOIDC4VP> getAuthorizationRequestFromJwtAuthorizationRequestJWT(String processId, String jwtAuthorizationRequestClaim) {
        try {
            // Step 1: Parse the JWT to extract the claims
            JWSObject jwsObject = JWSObject.parse(jwtAuthorizationRequestClaim);
            Map<String, Object> authorizationRequestClaim = getAuthorizationRequestClaim(jwsObject);

            // Step 4: Convert the map to a JSON string
            String jsonString = objectMapper.writeValueAsString(authorizationRequestClaim);

            // Step 5: Deserialize the JSON string into AuthorizationRequestOIDC4VP
            return Mono.fromCallable(() -> objectMapper.readValue(jsonString, AuthorizationRequestOIDC4VP.class));

        } catch (Exception e) {
            log.error("ProcessID: {} - Error while parsing Authorization Request: {}", processId, e.getMessage());
            return Mono.error(new RuntimeException("Error while parsing Authorization Request"));
        }
    }

    private static Map<String, Object> getAuthorizationRequestClaim(JWSObject jwsObject) {
        Map<String, Object> authorizationRequestClaim = jwsObject.getPayload().toJSONObject();
        // Step 2: Check if the "scope" claim exists and is a single string
        if (authorizationRequestClaim.containsKey(SCOPE_CLAIM) && authorizationRequestClaim.get(SCOPE_CLAIM) instanceof String scopeString) {

            // Step 3: Split the scope string into a list of strings
            List<String> scopeList = Arrays.asList(scopeString.split(" "));
            authorizationRequestClaim.put(SCOPE_CLAIM, scopeList); // Replace the string with the list in the map
        }
        return authorizationRequestClaim;
    }


}
