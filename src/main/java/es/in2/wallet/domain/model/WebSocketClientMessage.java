package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record WebSocketClientMessage(
        @JsonProperty("id")
        String id,
        @JsonProperty("pin")
        String pin
) {
}

