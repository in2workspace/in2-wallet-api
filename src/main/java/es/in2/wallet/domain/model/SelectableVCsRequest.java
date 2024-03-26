package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record SelectableVCsRequest(@JsonProperty("vcTypes") List<String> vcTypes) {
}

