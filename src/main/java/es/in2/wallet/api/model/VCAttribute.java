package es.in2.wallet.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record VCAttribute(
        @JsonProperty("id") String id,
        @JsonProperty("type") String type,
        @JsonProperty("value") Object value
) {

}