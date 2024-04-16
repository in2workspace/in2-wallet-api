package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CredentialTypeAttribute(
        @JsonProperty("type") String type,
        @JsonProperty("value") List<String> value
) {
}
