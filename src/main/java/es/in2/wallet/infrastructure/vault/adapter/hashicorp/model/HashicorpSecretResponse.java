package es.in2.wallet.infrastructure.vault.adapter.hashicorp.model;

import java.util.Map;

public record HashicorpSecretResponse(Map<String, String> data, Map<String, String> metadata) {
    @SuppressWarnings("unchecked")
    public HashicorpSecretResponse(Map<String, Object> response) {
        this((Map<String, String>) response.get("data"), (Map<String, String>) response.get("metadata"));
    }
}
