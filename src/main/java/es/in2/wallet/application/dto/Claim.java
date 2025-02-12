package es.in2.wallet.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record Claim(
        @JsonProperty("name") String name,
        @JsonProperty("allowedValues") List<Object> allowedValues
) {}
