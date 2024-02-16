package es.in2.wallet.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record CredentialRequest(
        // todo: this attribute is used by EBSI but not by IN2
//        @JsonProperty("types") List<String> types,
        @JsonProperty("format") String format,
        @JsonProperty("proof") Proof proof
) {
    @Builder
    public record Proof(
            // fixme: issuer doen't understand prrof_type
//        @JsonProperty("proof_type")
            String proofType,
            @JsonProperty("jwt")
            String jwt) {
    }
}
