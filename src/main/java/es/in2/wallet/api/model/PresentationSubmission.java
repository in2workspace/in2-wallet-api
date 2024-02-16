package es.in2.wallet.api.model;

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
    @Builder
    public record DescriptorMap(
            @JsonProperty("id")
            String id,
            @JsonProperty("format")
            String format,
            @JsonProperty("path")
            String path,
            @JsonProperty("path_nested")
            DescriptorMap pathNested
    ){

    }
}