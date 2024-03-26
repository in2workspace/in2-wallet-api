package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record VerifiablePresentation(
        @JsonProperty("@context")
        List<String> context,

        @JsonProperty("id")
        String id,

        @JsonProperty("type")
        List<String> type,

        @JsonProperty("holder")
        String holder,

        @JsonProperty("verifiableCredential")
        List<String> verifiableCredential
) {
}
