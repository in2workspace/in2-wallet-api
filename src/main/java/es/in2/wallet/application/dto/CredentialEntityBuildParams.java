package es.in2.wallet.application.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Builder
public record CredentialEntityBuildParams(
        UUID credentialId,
        UUID userId,
        List<String> credentialTypes,
        Integer credentialFormat,
        String credentialData,
        JsonNode vcJson,
        int credentialStatus,
        Timestamp timestamp
) {}

