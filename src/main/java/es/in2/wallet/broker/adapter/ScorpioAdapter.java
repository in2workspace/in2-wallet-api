package es.in2.wallet.broker.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.api.exception.JsonReadingException;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class ScorpioAdapter implements GenericBrokerService {

    private final ObjectMapper objectMapper;
    private final BrokerProperties brokerProperties;
    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder().baseUrl(brokerProperties.externalDomain()).build();
    }

    @Override
    public Mono<Void> postEntity(String processId, String authToken, String requestBody) {
        MediaType mediaType = getContentTypeAndAcceptMediaType(requestBody);
        return webClient.post()
                .uri(brokerProperties.paths().entities())
                .accept(mediaType)
                .contentType(mediaType)
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
        MediaType mediaType = getContentTypeAndAcceptMediaType(requestBody);
        return webClient.patch()
                            .uri(brokerProperties.paths().entities() + ENTITY_PREFIX + userId + ATTRIBUTES)
                            .accept(mediaType)
                            .contentType(mediaType)
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToMono(Void.class)
                .doOnSuccess(result -> log.info(RESOURCE_UPDATED_MESSAGE, processId))
                .doOnError(e -> log.error(ERROR_UPDATING_RESOURCE_MESSAGE, e.getMessage()));
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
