package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record TimeRange(
        @JsonProperty("from") String from,
        @JsonProperty("to") String to
) {}
