package es.in2.wallet.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record CredentialTypeAttribute(
        @JsonProperty("type") String type,
        @JsonProperty("value") List<String> value
) {
}
