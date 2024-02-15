package es.in2.wallet.api.ebsi.comformance.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.api.ebsi.comformance.configuration.properties.IdentityProviderProperties;
import es.in2.wallet.api.service.DidKeyGeneratorService;
import es.in2.wallet.api.service.KeyGenerationService;
import es.in2.wallet.api.service.UserDataService;
import es.in2.wallet.broker.service.BrokerService;
import es.in2.wallet.vault.service.VaultService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import static es.in2.wallet.api.util.MessageUtils.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class EbsiConfig
{
    private final ObjectMapper objectMapper;
    private final IdentityProviderProperties identityProviderProperties;
    private final KeyGenerationService keyGenerationService;
    private final DidKeyGeneratorService didKeyGeneratorService;
    private final VaultService vaultService;
    private final BrokerService brokerService;
    private final UserDataService userDataService;


    private String didForEbsi;
    @PostConstruct
    @Tag(name = "EbsiConfig", description = "Generate Did for ebsi purposes")
    public void init(){
        generateDid().subscribe(did -> this.didForEbsi = did);
    }
    private Mono<String> generateDid(){
        String processId = UUID.randomUUID().toString();
        MDC.put("processId", processId);
        List<Map.Entry<String, String>> headers = new ArrayList<>();

        log.debug(headers.toString());

        headers.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_URL_ENCODED_FORM));

        String body = "grant_type=" + URLEncoder.encode("password", StandardCharsets.UTF_8) +
                "&username=" + URLEncoder.encode(identityProviderProperties.username(), StandardCharsets.UTF_8) +
                "&password=" + URLEncoder.encode(identityProviderProperties.password(), StandardCharsets.UTF_8) +
                "&client_id=" + URLEncoder.encode(identityProviderProperties.clientId(), StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(identityProviderProperties.clientSecret(), StandardCharsets.UTF_8);

        return Mono.delay(Duration.ofSeconds(5))
                .then(postRequest(identityProviderProperties.url(),headers,body))
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
                }).flatMap(token -> generateAndSaveKeyPair()
                        .flatMap(map -> createAndUpdateUser(processId,token,map.get("did"))
                                .thenReturn(map.get("did"))))
                .onErrorResume(e -> {
                    log.error("Error while processing did generation: {}", e.getMessage());

                    return Mono.error(new RuntimeException("The user already exist"));
                }
                );


    }
    private Mono<Map<String, String>> generateAndSaveKeyPair() {
        return keyGenerationService.generateES256r1ECKeyPair()
                .flatMap(didKeyGeneratorService::generateDidKeyJwkJcsPubWithFromKeyPair)
                .flatMap(map -> vaultService.saveSecret(map).thenReturn(map));
    }

    private Mono<Void> createAndUpdateUser(String processId, String authorizationToken, String did) {
        return getUserIdFromToken(authorizationToken)
                .flatMap(userId -> userDataService.createUserEntity(userId)
                        .flatMap(createdUserId -> brokerService.postEntity(processId, createdUserId))
                        .then(brokerService.getEntityById(processId, userId))
                        .flatMap(optionalEntity ->
                                optionalEntity.map(entity ->
                                                userDataService.saveDid(entity, did, "did:key")
                                                        .flatMap(didUpdatedEntity -> brokerService.updateEntity(processId, userId, didUpdatedEntity))
                                        )
                                        .orElseGet(() -> Mono.error(new RuntimeException("Entity not found after creation."))
                                        )));
    }
    public Mono<String> getDid() {
        return Mono.just(this.didForEbsi);
    }
}
