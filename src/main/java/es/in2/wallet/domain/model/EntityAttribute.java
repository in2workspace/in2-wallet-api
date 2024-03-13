package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record EntityAttribute<T>(
        @JsonProperty("type") String type,
        @JsonProperty("value") T value
) {

}
