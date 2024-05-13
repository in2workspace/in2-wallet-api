package es.in2.wallet.infrastructure.broker.adapter;

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
import static es.in2.wallet.domain.util.MessageUtils.ERROR_UPDATING_RESOURCE_MESSAGE;
import static es.in2.wallet.domain.util.MessageUtils.RESOURCE_UPDATED_MESSAGE;


@Component
@Slf4j
@RequiredArgsConstructor
public class OrionLdAdapter implements GenericBrokerService {

    private final BrokerConfig brokerConfig;
    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = ApplicationUtils.WEB_CLIENT;
    }

    @Override
    public Mono<Void> postEntity(String processId, String requestBody) {
        return webClient.post()
                .uri(brokerConfig.getExternalUrl() + brokerConfig.getEntitiesPath())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Void.class);
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
                .uri(brokerConfig.getExternalUrl() + brokerConfig.getEntitiesPath() + "?type=Credential&q=belongsTo==" + USER_ENTITY_PREFIX + userId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.error(new NoSuchVerifiableCredentialException("Error fetching credentials from user: " + userId)));
    }

    @Override
    public Mono<String> getCredentialByIdAndUserId(String processId, String credentialId, String userId) {
        return webClient.get()
                .uri(brokerConfig.getExternalUrl() + brokerConfig.getEntitiesPath() + "/" + credentialId + "?q=belongsTo==" + USER_ENTITY_PREFIX + userId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.error(new NoSuchVerifiableCredentialException("Error fetching credentials from user: " + userId)));
    }

    @Override
    public Mono<Void> deleteCredentialByIdAndUserId(String processId, String credentialId, String userId) {
        return webClient.delete()
                .uri(brokerConfig.getExternalUrl() + brokerConfig.getEntitiesPath() + "/" + credentialId + "?q=belongsTo==" + USER_ENTITY_PREFIX + userId)
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
                .onErrorResume(e -> Mono.error(new NoSuchVerifiableCredentialException(
                        "Error fetching credentials of type: " + credentialType + " from user: " + userId)));
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
        return webClient.post()
                .uri(brokerConfig.getExternalUrl() + brokerConfig.getEntitiesPath() + "/" + id + ATTRIBUTES)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(result -> log.info(RESOURCE_UPDATED_MESSAGE, processId))
                .doOnError(e -> log.error(ERROR_UPDATING_RESOURCE_MESSAGE, e.getMessage()));
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

}
