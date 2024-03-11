package es.in2.wallet.infrastructure.broker.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.domain.exception.JsonReadingException;
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

import static es.in2.wallet.domain.util.MessageUtils.ATTRIBUTES;
import static es.in2.wallet.domain.util.MessageUtils.ENTITY_PREFIX;

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
                .uri(brokerConfig.getExternalUrl() + brokerConfig.getPathEntities())
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
    public Mono<Optional<String>> getEntityById(String processId, String userId) {
        return webClient.get()
                .uri(brokerConfig.getExternalUrl() + brokerConfig.getPathEntities() + ENTITY_PREFIX + userId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status != null && status.is4xxClientError(), response -> response.createException().flatMap(Mono::error))
                .bodyToMono(String.class)
                .map(Optional::of)
                .doOnNext(body -> log.info("Response body: {}", body))
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.just(Optional.empty())) // Maneja específicamente el caso 404 aquí
                .defaultIfEmpty(Optional.empty()); // Maneja el caso en que la respuesta es exitosa pero no hay cuerpo
    }




    @Override
    public Mono<Void> updateEntity(String processId, String userId, String requestBody) {
        MediaType mediaType = getContentTypeAndAcceptMediaType(requestBody);
        return webClient.patch()
                .uri(brokerConfig.getExternalUrl() + brokerConfig.getPathEntities() + ENTITY_PREFIX + userId + ATTRIBUTES)
                .accept(mediaType)
                .contentType(mediaType)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.debug("Entity updated"))
                .doOnError(e -> log.debug("Error updating entity"))
                .onErrorResume(Exception.class, Mono::error);
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
