package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CredentialAttribute (
        @JsonProperty("type") String type,
        @JsonProperty("value") Object value
){
}
