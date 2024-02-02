package es.in2.wallet.api.crypto.domain;

import java.util.List;
import java.util.Map;

public record VerifiableCredential (
    List<String> type,
    List<String> context,
    String id,
    String issuer,
    String issuanceDate,
    String expirationDate,
    Map<String, Object> proof,
    Map<String, Object> credentialSchema,
    Map<String, Object> credentialSubject,
    Map<String, Object> properties
)
{}

