package es.in2.wallet.broker.adapter;

import es.in2.wallet.broker.config.properties.BrokerProperties;
import es.in2.wallet.broker.service.GenericBrokerService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static es.in2.wallet.api.util.MessageUtils.*;


@Component
@Slf4j
@RequiredArgsConstructor
public class OrionLdAdapter implements GenericBrokerService {

    private final BrokerProperties brokerProperties;
    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder().baseUrl(brokerProperties.externalDomain()).build();
    }

    @Override
    public Mono<Void> postEntity(String processId, String authToken, String requestBody) {
        return webClient.post()
                .uri(brokerProperties.paths().entities())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Void.class);
    }

    @Override
    public Mono<String> getEntityById(String processId, String userId) {

        return webClient.get()
                .uri(brokerProperties.paths().entities() + ENTITY_PREFIX + userId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status != null && status.is4xxClientError(), response -> {
                    if (response.statusCode().equals(HttpStatus.NOT_FOUND)) {
                        return Mono.empty();
                    }
                    return response.createException()
                            .flatMap(Mono::error);
                })
                .bodyToMono(String.class)
                .onErrorResume(Exception.class, Mono::error);
    }

    @Override
    public Mono<Void> updateEntity(String processId, String userId, String requestBody) {
        return webClient.patch()
                .uri(brokerProperties.paths().entities() + ENTITY_PREFIX + userId + ATTRIBUTES)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(result -> log.info(RESOURCE_UPDATED_MESSAGE, processId))
                .doOnError(e -> log.error(ERROR_UPDATING_RESOURCE_MESSAGE, e.getMessage()));
    }


}
