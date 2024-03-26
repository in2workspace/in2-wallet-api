package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record PresentationSubmission(
        @JsonProperty("id")
        String id,
        @JsonProperty("definition_id")
        String definitionId,
        @JsonProperty("descriptor_map")
        List<DescriptorMap> descriptorMap
) {
}
