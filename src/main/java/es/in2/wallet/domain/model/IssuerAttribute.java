package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record IssuerAttribute(
        @JsonProperty("hash")
        String hash,
        @JsonProperty("body")
        String body,
        @JsonProperty("issuerType")
        String issuerType
) {
}