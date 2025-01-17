package es.in2.wallet.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record EntityAttribute<T>(
        @JsonProperty("type") String type,
        @JsonProperty("value") T value
) {

}
