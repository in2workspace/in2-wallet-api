package es.in2.wallet.infrastructure.broker.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.domain.exception.JsonReadingException;
import es.in2.wallet.domain.exception.NoSuchTransactionException;
import es.in2.wallet.domain.exception.NoSuchVerifiableCredentialException;
import es.in2.wallet.domain.util.ApplicationUtils;
import es.in2.wallet.infrastructure.broker.config.BrokerConfig;
import es.in2.wallet.infrastructure.broker.service.GenericBrokerService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static es.in2.wallet.domain.util.ApplicationConstants.ATTRIBUTES;
import static es.in2.wallet.domain.util.ApplicationConstants.USER_ENTITY_PREFIX;


@Slf4j
@Component
@RequiredArgsConstructor
public class ScorpioAdapter implements GenericBrokerService {

    private final ObjectMapper objectMapper;
    private final BrokerConfig brokerConfig;
    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = ApplicationUtils.WEB_CLIENT;
    }

    @Override
    public Mono<Void> postEntity(String processId, String requestBody) {
        MediaType mediaType = getContentTypeAndAcceptMediaType(requestBody);
        return webClient.post()
                .uri(brokerConfig.getExternalUrl() + brokerConfig.getEntitiesPath())
                .accept(mediaType)
                .contentType(mediaType)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.debug("Entity saved"))
                .doOnError(e -> log.debug("Error saving entity"))
                .onErrorResume(Exception.class, Mono::error);
    }

    @Override
    public Mono<Optional<String>> getEntityById(String processId, String id) {
        return webClient.get()
                .uri(brokerConfig.getExternalUrl() + brokerConfig.getEntitiesPath() + "/" + id)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status != null && status.is4xxClientError(), response -> response.createException().flatMap(Mono::error))
                .bodyToMono(String.class)
                .map(Optional::of)
                .doOnNext(body -> log.info("Response body: {}", body))
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.just(Optional.empty())) // Specifically handle the 404 case here
                .defaultIfEmpty(Optional.empty()); // Handle the case where the response is successful but there is no body
    }

    @Override
    public Mono<String> getAllCredentialsByUserId(String processId, String userId) {
        return webClient.get()
                .uri(brokerConfig.getExternalUrl() + brokerConfig.getEntitiesPath() +
                        "?type=Credential&q=belongsTo==" + USER_ENTITY_PREFIX + userId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.error(new NoSuchVerifiableCredentialException("Error fetching credentials from user: " + userId)));
    }

    @Override
    public Mono<String> getCredentialByIdAndUserId(String processId, String credentialId, String userId) {
        return webClient.get()
                .uri(brokerConfig.getExternalUrl() + brokerConfig.getEntitiesPath() +
                        "/" + credentialId + "?q=belongsTo==" + USER_ENTITY_PREFIX + userId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.error(new NoSuchVerifiableCredentialException("Error fetching credential with id: " + credentialId + " from user: " + userId)));
    }

    @Override
    public Mono<Void> deleteCredentialByIdAndUserId(String processId, String credentialId, String userId) {
        return webClient.delete()
                .uri(brokerConfig.getExternalUrl() + brokerConfig.getEntitiesPath() +
                        "/" + credentialId + "?q=belongsTo==" + USER_ENTITY_PREFIX + userId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> Mono.error(new NoSuchVerifiableCredentialException("Error deleting credential with id: " + credentialId + " from user: " + userId)));
    }

    @Override
    public Mono<String> getCredentialByCredentialTypeAndUserId(String processId, String credentialType, String userId) {
        return webClient.get()
                .uri(brokerConfig.getExternalUrl() + brokerConfig.getEntitiesPath() +
                        "?type=Credential&q=belongsTo==" + USER_ENTITY_PREFIX + userId + ";credentialType==" + credentialType)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.error(new NoSuchVerifiableCredentialException("Error fetching credentials from user: " + userId)));
    }

    @Override
    public Mono<String> getTransactionThatIsLinkedToACredential(String processId, String credentialId) {
        return webClient.get()
                .uri(brokerConfig.getExternalUrl() + brokerConfig.getEntitiesPath() + "?type=Transaction&q=linkedTo==" + credentialId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.error(new NoSuchTransactionException("Error fetching transaction from credential: " + credentialId)));
    }


    @Override
    public Mono<Void> updateEntityById(String processId, String id, String requestBody) {
        MediaType mediaType = getContentTypeAndAcceptMediaType(requestBody);
        return webClient.post()
                .uri(brokerConfig.getExternalUrl() + brokerConfig.getEntitiesPath() + "/" + id + ATTRIBUTES)
                .accept(mediaType)
                .contentType(mediaType)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.debug("Entity updated"))
                .doOnError(e -> log.debug("Error updating entity"))
                .onErrorResume(Exception.class, Mono::error);
    }

    @Override
    public Mono<Void> deleteTransactionByTransactionId(String processId, String transactionId) {
        return webClient.delete()
                .uri(brokerConfig.getExternalUrl() + brokerConfig.getEntitiesPath() +
                        "/" + transactionId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> Mono.error(new NoSuchVerifiableCredentialException("Error deleting transaction with id: " + transactionId)));
    }

    private MediaType getContentTypeAndAcceptMediaType(String requestBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(requestBody);
            if (jsonNode.has("@context")) {
                return MediaType.valueOf("application/ld+json");
            } else {
                return MediaType.APPLICATION_JSON;
            }
        } catch (JsonProcessingException e) {
            throw new JsonReadingException(e.getMessage());
        }
    }

}
