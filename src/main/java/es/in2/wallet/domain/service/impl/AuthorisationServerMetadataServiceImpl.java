package es.in2.wallet.domain.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.application.port.AppConfig;
import es.in2.wallet.domain.exception.FailedDeserializingException;
import es.in2.wallet.domain.model.AuthorisationServerMetadata;
import es.in2.wallet.domain.model.CredentialIssuerMetadata;
import es.in2.wallet.domain.service.AuthorisationServerMetadataService;
import es.in2.wallet.infrastructure.core.config.WebClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static es.in2.wallet.domain.util.ApplicationConstants.CONTENT_TYPE;
import static es.in2.wallet.domain.util.ApplicationConstants.CONTENT_TYPE_APPLICATION_JSON;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorisationServerMetadataServiceImpl implements AuthorisationServerMetadataService {
    private final ObjectMapper objectMapper;
    private final AppConfig appConfig;
    private final WebClientConfig webClient;

    @Override
    public Mono<AuthorisationServerMetadata> getAuthorizationServerMetadataFromCredentialIssuerMetadata(String processId, CredentialIssuerMetadata credentialIssuerMetadata) {
        // get Credential Issuer Metadata
        return getAuthorizationServerMetadata(credentialIssuerMetadata)
                .doOnSuccess(response -> log.info("ProcessID: {} - Authorisation Server Metadata Response: {}", processId, response))
                .flatMap(this::parseCredentialIssuerMetadataResponse)
                .doOnNext(authorisationServerMetadata -> log.info("ProcessID: {} - AuthorisationServerMetadata: {}", processId, authorisationServerMetadata))
                .onErrorResume(e -> {
                    log.error("ProcessID: {} - Error while processing Authorisation Server Metadata Response from the Auth Server: {}", processId, e.getMessage());
                    return Mono.error(new RuntimeException("Error while processing Authorisation Server Metadata Response from the Auth Server. Reason: " + e.getMessage()));
                });
    }

    private Mono<String> getAuthorizationServerMetadata(CredentialIssuerMetadata credentialIssuerMetadata) {
        String authServer;
        if (credentialIssuerMetadata.authorizationServer() != null){
            authServer = credentialIssuerMetadata.authorizationServer();
        }
        else {
            authServer = credentialIssuerMetadata.credentialIssuer();
        }
        return webClient.centralizedWebClient()
                .get()
                .uri(authServer + "/.well-known/openid-configuration")
                .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                .exchangeToMono(response -> {
                    if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                        return Mono.error(new RuntimeException("There was an error retrieving authorisation server metadata, error" + response));
                    }
                    else {
                        log.info("Authorization server metadata: {}", response);
                        return response.bodyToMono(String.class);
                    }
                });
    }

    /**
     * This method is marked as deprecated and will be replaced in the future.
     * The current implementation includes hardcoded token endpoint logic to maintain
     * backward compatibility with our wallet. A refactoring is planned to improve
     * this method.
     *
     * @param response The response String to be parsed.
     * @return An instance of Mono<AuthorisationServerMetadata>.
     * @deprecated (since = " 1.0.0 ", forRemoval = true) This implementation is temporary and should be replaced in future versions.
     */
    @Deprecated(since = ".0.0", forRemoval = true)
    private Mono<AuthorisationServerMetadata> parseCredentialIssuerMetadataResponse(String response) {
        try {
            AuthorisationServerMetadata authorisationServerMetadata = objectMapper.readValue(response, AuthorisationServerMetadata.class);
            if (authorisationServerMetadata.tokenEndpoint().startsWith(appConfig.getAuthServerExternalUrl())) {
                AuthorisationServerMetadata authorisationServerMetadataWithTokenEndpointHardcoded = AuthorisationServerMetadata.builder()
                        .issuer(authorisationServerMetadata.issuer())
                        .authorizationEndpoint(authorisationServerMetadata.authorizationEndpoint())
                        .tokenEndpoint(appConfig.getAuthServerTokenEndpoint())
                        .build();
                return Mono.just(authorisationServerMetadataWithTokenEndpointHardcoded);
            }

            // deserialize Credential Issuer Metadata
            return Mono.just(authorisationServerMetadata);
        } catch (Exception e) {
            return Mono.error(new FailedDeserializingException("Error while deserializing Credential Issuer Metadata. Reason: " + e.getMessage()));
        }
    }
}
