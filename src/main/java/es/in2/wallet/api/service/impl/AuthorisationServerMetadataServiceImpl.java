package es.in2.wallet.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.api.config.AppConfig;
import es.in2.wallet.api.exception.FailedCommunicationException;
import es.in2.wallet.api.exception.FailedDeserializingException;
import es.in2.wallet.api.model.AuthorisationServerMetadata;
import es.in2.wallet.api.model.CredentialIssuerMetadata;
import es.in2.wallet.api.service.AuthorisationServerMetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static es.in2.wallet.api.util.ApplicationUtils.getRequest;
import static es.in2.wallet.api.util.MessageUtils.CONTENT_TYPE;
import static es.in2.wallet.api.util.MessageUtils.CONTENT_TYPE_APPLICATION_JSON;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorisationServerMetadataServiceImpl implements AuthorisationServerMetadataService {
    private final ObjectMapper objectMapper;
    private final AppConfig appConfig;

    @Override
    public Mono<AuthorisationServerMetadata> getAuthorizationServerMetadataFromCredentialIssuerMetadata(String processId, CredentialIssuerMetadata credentialIssuerMetadata) {
        String authorizationServiceURL = credentialIssuerMetadata.authorizationServer() + "/.well-known/openid-configuration";
        // get Credential Issuer Metadata
        return getAuthorizationServerMetadata(authorizationServiceURL)
                .doOnSuccess(response -> log.info("ProcessID: {} - Authorisation Server Metadata Response: {}", processId, response))
                .flatMap(this::parseCredentialIssuerMetadataResponse)
                .doOnNext(authorisationServerMetadata -> log.info("ProcessID: {} - AuthorisationServerMetadata: {}", processId, authorisationServerMetadata))
                .onErrorResume(e -> {
                    log.error("ProcessID: {} - Error while processing Authorisation Server Metadata Response from the Auth Server: {}", processId, e.getMessage());
                    return Mono.error(new RuntimeException("Error while processing Authorisation Server Metadata Response from the Auth Server. Reason: " + e.getMessage()));
                });
    }

    private Mono<String> getAuthorizationServerMetadata(String authorizationServerURL) {
        List<Map.Entry<String, String>> headers = List.of(Map.entry(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));
        return getRequest(authorizationServerURL, headers)
                .onErrorResume(e -> Mono.error(new FailedCommunicationException("Error while fetching Authorisation Server Metadata from the Auth Server. Reason: " + e.getMessage())));
    }

    /**
     * This method is marked as deprecated and will be replaced in the future.
     * The current implementation includes hardcoded token endpoint logic to maintain
     * backward compatibility with our wallet. A refactoring is planned to improve
     * this method.
     *
     * @param response The response String to be parsed.
     * @return An instance of Mono<AuthorisationServerMetadata>.
     * @deprecated (since = "1.0.0", forRemoval = true) This implementation is temporary and should be replaced in future versions.
     */
    @Deprecated(since = ".0.0", forRemoval = true)
    private Mono<AuthorisationServerMetadata> parseCredentialIssuerMetadataResponse(String response) {
        try {
            AuthorisationServerMetadata authorisationServerMetadata = objectMapper.readValue(response, AuthorisationServerMetadata.class);
            if (authorisationServerMetadata.tokenEndpoint().startsWith(appConfig.getAuthServerExternalDomain())){
                AuthorisationServerMetadata authorisationServerMetadataWithTokenEndpointHardcoded = AuthorisationServerMetadata.builder()
                        .issuer(authorisationServerMetadata.issuer())
                        .authorizationEndpoint(authorisationServerMetadata.authorizationEndpoint())
                        .tokenEndpoint(appConfig.getAuthServerTokenEndpoint())
                        .build();
                return Mono.just(authorisationServerMetadataWithTokenEndpointHardcoded);
            }

            // deserialize Credential Issuer Metadata
            return Mono.just(authorisationServerMetadata);
        }
        catch (Exception e) {
            return Mono.error(new FailedDeserializingException("Error while deserializing Credential Issuer Metadata. Reason: " + e.getMessage()));
        }
    }
}
