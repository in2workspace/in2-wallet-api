package es.in2.wallet.infrastructure.broker.adapter;

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

import static es.in2.wallet.domain.util.MessageUtils.*;


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
    public Mono<Optional<String>> getEntityById(String processId, String userId) {
        return webClient.get()
                .uri( brokerConfig.getExternalUrl() + brokerConfig.getEntitiesPath() + ENTITY_PREFIX + userId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status != null && status.is4xxClientError(), response -> response.createException().flatMap(Mono::error))
                .bodyToMono(String.class)
                .map(Optional::of)
                .doOnNext(body -> log.info("Response body: {}", body))
                .doOnError(error -> log.error("Error occurred: ", error))
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.just(Optional.empty()))
                .defaultIfEmpty(Optional.empty());
    }

    @Override
    public Mono<Void> updateEntity(String processId, String userId, String requestBody) {
        return webClient.patch()
                .uri(brokerConfig.getExternalUrl() + brokerConfig.getEntitiesPath() + ENTITY_PREFIX + userId + ATTRIBUTES)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(result -> log.info(RESOURCE_UPDATED_MESSAGE, processId))
                .doOnError(e -> log.error(ERROR_UPDATING_RESOURCE_MESSAGE, e.getMessage()));
    }


}
