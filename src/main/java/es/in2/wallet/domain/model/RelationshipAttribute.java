package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record RelationshipAttribute (
        @JsonProperty("type") String type,
        @JsonProperty("object") String object
){
}
