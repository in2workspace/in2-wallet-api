package es.in2.wallet.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CredentialAttribute (
        @JsonProperty("type") String type,
        @JsonProperty("value") Object value
){
}
