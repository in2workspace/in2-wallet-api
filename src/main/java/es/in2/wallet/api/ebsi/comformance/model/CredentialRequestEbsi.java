package es.in2.wallet.api.ebsi.comformance.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record CredentialRequestEbsi(
        @JsonProperty("types") List<String> types,

        @JsonProperty("format") String format,

        @JsonProperty("proof") Proof proof
) {
    @Builder
    public record Proof(
            @JsonProperty("proof_type") String proofType,

            @JsonProperty("jwt") String jwt
    ) {
    }
}
