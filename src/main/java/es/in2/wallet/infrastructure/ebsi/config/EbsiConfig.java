package es.in2.wallet.infrastructure.ebsi.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.application.port.AppConfig;
import es.in2.wallet.application.port.BrokerService;
import es.in2.wallet.domain.model.CredentialEntity;
import es.in2.wallet.domain.service.DataService;
import es.in2.wallet.domain.service.DidKeyGeneratorService;
import es.in2.wallet.domain.util.ApplicationUtils;
import es.in2.wallet.infrastructure.core.config.WebClientConfig;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static es.in2.wallet.domain.util.ApplicationConstants.*;

@Component
@RequiredArgsConstructor
@Slf4j
@Tag(name = "EbsiConfig", description = "Generate Did for ebsi purposes")
public class EbsiConfig {

    private final ObjectMapper objectMapper;
    private final AppConfig appConfig;
    private final DidKeyGeneratorService didKeyGeneratorService;
    private final BrokerService brokerService;
    private final DataService dataService;
    private final WebClientConfig webClient;
    private String didForEbsi;

    @PostConstruct
    public void onPostConstruct() {
        init().subscribe(
                null,
                error -> log.error("Initialization failed", error)
        );
    }

    public Mono<String> init() {
        return generateEbsiDid()
                .doOnNext(did -> this.didForEbsi = did)
                .doOnError(error -> log.error("Initialization failed", error));
    }

    private Mono<String> generateEbsiDid() {
        String processId = UUID.randomUUID().toString();
        String credentialId = "urn:entities:credential:exampleCredential";
        String vcType = "ExampleCredential";

        String clientSecret = appConfig.getIdentityProviderClientSecret().trim();
        String decodedSecret;

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(clientSecret);
            decodedSecret = new String(decodedBytes, StandardCharsets.UTF_8);

            String reEncodedSecret = Base64.getEncoder().encodeToString(decodedSecret.getBytes(StandardCharsets.UTF_8)).trim();
            if (!clientSecret.equals(reEncodedSecret)) {
                decodedSecret = clientSecret;
            }
        } catch (IllegalArgumentException ex) {
            decodedSecret = clientSecret;
        }

        String body = "grant_type=" + URLEncoder.encode("password", StandardCharsets.UTF_8) +
                "&username=" + URLEncoder.encode(appConfig.getIdentityProviderUsername(), StandardCharsets.UTF_8) +
                "&password=" + URLEncoder.encode(appConfig.getIdentityProviderPassword(), StandardCharsets.UTF_8) +
                "&client_id=" + URLEncoder.encode(appConfig.getIdentityProviderClientId(), StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(decodedSecret, StandardCharsets.UTF_8);

        return Mono.delay(Duration.ofSeconds(15))
                .then(webClient.centralizedWebClient()
                        .post()
                        .uri(appConfig.getIdentityProviderUrl())
                        .header(CONTENT_TYPE, CONTENT_TYPE_URL_ENCODED_FORM)
                        .bodyValue(body)
                        .exchangeToMono(response -> {
                            if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                                return Mono.error(new RuntimeException("Error getting token from user: " + appConfig.getIdentityProviderUsername()));
                            } else {
                                log.info("Token retrieval completed");
                                return response.bodyToMono(String.class);
                            }
                        }))
                .flatMap(response -> {
                    log.debug(response);
                    Map<String, Object> jsonObject;
                    try {
                        jsonObject = objectMapper.readValue(response, new TypeReference<>() {});
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException(e));
                    }
                    String token = jsonObject.get("access_token").toString();
                    return Mono.just(token);
                })
                .flatMap(ApplicationUtils::getUserIdFromToken)
                .flatMap(userId -> brokerService.getEntityById(processId, USER_ENTITY_PREFIX + userId)
                        .flatMap(optionalEntity -> optionalEntity
                                .map(entity -> getDidForUserCredential(processId, userId, vcType))
                                .orElseGet(() -> generateDid()
                                        .flatMap(did -> createAndAddCredentialWithADidToPassEbsiTest(processId, userId, did, credentialId, vcType)
                                                .thenReturn(did)
                                        )
                                )
                        )
                )
                .doOnError(e -> log.error("Error while processing did generation: {}", e.getMessage()))
                .onErrorResume(e -> Mono.error(new RuntimeException("The user already exists: " + e)));
    }

    private Mono<String> generateDid() {
        return didKeyGeneratorService.generateDidKeyJwkJcsPub();
    }

    private Mono<Void> createAndAddCredentialWithADidToPassEbsiTest(String processId, String userId, String did, String credentialId, String type) {
        String credentialEntity = String.format("""
                            {
                              "id": "%s",
                              "type": "Credential",
                              "status": {
                                "type": "Property",
                                "value": "ISSUED"
                              },
                              "credentialType": {
                                "type": "Property",
                                "value": ["%s", "VerifiableCredential"]
                              },
                              "json_vc": {
                                "type": "Property",
                                "value": {
                                  "id": "urn:credential:exampleCredential",
                                  "credentialSubject": {
                                    "id" : "%s"
                                  }
                                }
                              },
                              "belongsTo": {
                                "type": "Relationship",
                                "object": "urn:entities:walletUser:%s"
                              }
                            }
                            """, credentialId, type, did, userId);
        return dataService.createUserEntity(userId)
                .flatMap(createdUserId -> brokerService.postEntity(processId, createdUserId))
                .then(brokerService.postEntity(processId, credentialEntity));
    }

    private Mono<String> getDidForUserCredential(String processId, String userId, String type) {
        return brokerService.getCredentialByCredentialTypeAndUserId(processId, type, userId)
                .flatMap(credentialsJson -> {
                    try {
                        List<CredentialEntity> credentials = objectMapper.readValue(credentialsJson, new TypeReference<>() {});
                        CredentialEntity firstCredential = credentials.get(0);
                        String firstCredentialJson = objectMapper.writeValueAsString(firstCredential);
                        return dataService.extractDidFromVerifiableCredential(firstCredentialJson);
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Error processing credentials", e));
                    }
                });
    }

    public Mono<String> getDid() {
        return Mono.just(this.didForEbsi);
    }
}


