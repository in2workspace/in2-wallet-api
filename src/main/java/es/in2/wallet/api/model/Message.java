package es.in2.wallet.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record Message(
        @JsonProperty("url")
        String url
) {
}
