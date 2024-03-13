package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;


@Builder
public record DescriptorMap(
    @JsonProperty("format") String format,

    @JsonProperty("path") String path,
    @JsonProperty("id") String id,
    @JsonProperty("path_nested") DescriptorMap pathNested
)
    {
}
