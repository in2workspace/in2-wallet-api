package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

import java.util.List;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record PresentationDefinition(
        @JsonProperty("id") String id,
        @JsonProperty("input_descriptors") List<InputDescriptor> inputDescriptors,
        @JsonProperty("format") Format format

) {
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InputDescriptor(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("purpose") String purpose,
            @JsonProperty("constraints") Constraint constraints,
            @JsonProperty("format") Format format
    ) {
        @Builder
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Constraint(
                @JsonProperty("fields") List<Field> fields
        ) {

            @Builder
            @JsonIgnoreProperties(ignoreUnknown = true)
            public record Field(
                    @JsonProperty("path") List<String> path,
                    @JsonProperty("filter") JsonNode filter
            )
            {
            }
        }
    }
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Format(
            @JsonProperty("jwt") FormatDetail jwt,
            @JsonProperty("jwt_vc") FormatDetail jwt_vc,
            @JsonProperty("jwt_vp") FormatDetail jwt_vp,
            @JsonProperty("ldp_vc") FormatDetail ldp_vc,
            @JsonProperty("ldp_vp") FormatDetail ldp_vp,
            @JsonProperty("ldp") FormatDetail ldp
    ) {
        @Builder
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record FormatDetail(
                @JsonProperty("alg") List<String> alg,
                @JsonProperty("proof_type") List<String> proof_type
        ) {
        }
    }
}


