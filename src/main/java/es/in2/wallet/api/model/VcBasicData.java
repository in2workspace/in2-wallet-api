package es.in2.wallet.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record VcBasicData(
        @JsonProperty("id") String id,

        @JsonProperty("vcType") List<String> vcType,

        @JsonProperty("credentialSubject") Object credentialSubject
) {
}