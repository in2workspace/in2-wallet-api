package es.in2.wallet.infrastructure.ebsi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.application.port.AppConfig;
import es.in2.wallet.application.port.BrokerService;
import es.in2.wallet.domain.service.DidKeyGeneratorService;
import es.in2.wallet.domain.service.UserDataService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class EbsiConfig {

//    private final ObjectMapper objectMapper;
//    private final AppConfig appConfig;
//    private final DidKeyGeneratorService didKeyGeneratorService;
//    private final BrokerService brokerService;
//    private final UserDataService userDataService;
//
//    private String didForEbsi;
//
//    @PostConstruct
//    @Tag(name = "EbsiConfig", description = "Generate Did for ebsi purposes")
//    public void init() {
//        generateEbsiDid().subscribe(did -> this.didForEbsi = did);
//    }

//    private Mono<String> generateEbsiDid() {
//
//        String processId = UUID.randomUUID().toString();
//        MDC.put("processId", processId);
//        List<Map.Entry<String, String>> headers = new ArrayList<>();
//
//        log.debug(headers.toString());
//
//        headers.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_URL_ENCODED_FORM));
//
//        String clientSecret = appConfig.getIdentityProviderClientSecret().trim();
//        String decodedSecret;
//
//        try {
//            // Attempt to decode the clientSecret assuming it is Base64 encoded.
//            byte[] decodedBytes = Base64.getDecoder().decode(clientSecret);
//            decodedSecret = new String(decodedBytes, StandardCharsets.UTF_8);
//
//            // Check if re-encoding the decoded result gives us the original string.
//            String reEncodedSecret = Base64.getEncoder().encodeToString(decodedSecret.getBytes(StandardCharsets.UTF_8)).trim();
//            if (!clientSecret.equals(reEncodedSecret)) {
//                // If re-encoding the decoded text does not match the original string,
//                // assume the original was not in Base64 and use it as it was.
//                decodedSecret = clientSecret;
//            }
//        } catch (IllegalArgumentException ex) {
//            // If an error occurs during decoding, assume the string was not in Base64.
//            decodedSecret = clientSecret;
//        }
//
//        String body = "grant_type=" + URLEncoder.encode("password", StandardCharsets.UTF_8) +
//                "&username=" + URLEncoder.encode(appConfig.getIdentityProviderUsername(), StandardCharsets.UTF_8) +
//                "&password=" + URLEncoder.encode(appConfig.getIdentityProviderPassword(), StandardCharsets.UTF_8) +
//                "&client_id=" + URLEncoder.encode(appConfig.getIdentityProviderClientId(), StandardCharsets.UTF_8) +
//                "&client_secret=" + URLEncoder.encode(decodedSecret, StandardCharsets.UTF_8);
//
//        return Mono.delay(Duration.ofSeconds(30))
//                .then(postRequest(appConfig.getIdentityProviderUrl(), headers, body))
//                .flatMap(response -> {
//                    log.debug(response);
//                    Map<String, Object> jsonObject;
//                    try {
//                        jsonObject = objectMapper.readValue(response, new TypeReference<>() {
//                        });
//                    } catch (JsonProcessingException e) {
//                        return Mono.error(new RuntimeException(e));
//                    }
//                    String token = jsonObject.get("access_token").toString();
//
//                    return Mono.just(token);
//                })
//                .flatMap(ApplicationUtils::getUserIdFromToken)
//                .flatMap(userId -> brokerService.getEntityById(processId, userId)
//                        .flatMap(optionalEntity -> optionalEntity
//                                .map(entity -> getDidForUserAlreadyCreated(processId, userId))
//                                .orElseGet(() ->
//                                        generateDid()
//                                                .flatMap(did -> createAndUpdateUser(processId, userId, did)
//                                                        .thenReturn(did)
//                                                )
//                                )
//                        )
//                )
//                .onErrorResume(e -> {
//                            log.error("Error while processing did generation: {}", e.getMessage());
//
//                            return Mono.error(new RuntimeException("The user already exist"));
//                        }
//                );
//
//
//    }
//
//    private Mono<String> generateDid() {
//        return didKeyGeneratorService.generateDidKeyJwkJcsPub();
//    }
//
//    private Mono<Void> createAndUpdateUser(String processId, String userId, String did) {
//        return userDataService.createUserEntity(userId)
//                .flatMap(createdUserId -> brokerService.postEntity(processId, createdUserId))
//                .then(brokerService.getEntityById(processId, userId))
//                .flatMap(optionalEntity ->
//                        optionalEntity.map(entity ->
//                                        userDataService.saveDid(entity, did, "did:key")
//                                                .flatMap(didUpdatedEntity -> brokerService.updateEntity(processId, userId, didUpdatedEntity))
//                                )
//                                .orElseGet(() -> Mono.error(new RuntimeException("Entity not found after creation."))
//                                ));
//    }
//
//    private Mono<String> getDidForUserAlreadyCreated(String processId, String userId) {
//        return brokerService.getEntityById(processId, userId)
//                .flatMap(optionalEntity ->
//                        optionalEntity.map(userDataService::getDidsByUserEntity)
//                                .orElseGet(() -> Mono.error(new RuntimeException("Entity not found after creation."))))
//                .flatMapIterable(dids -> dids)
//                .next();
//    }

    public Mono<String> getDid() {
        return Mono.just(
//                this.didForEbsi
                "did:key:example"
        );
    }

}
