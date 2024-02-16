package es.in2.wallet.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record ClientMessage(
        @JsonProperty("id")
        String id,
        @JsonProperty("pin")
        String pin
) {
}

