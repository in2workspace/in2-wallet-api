package es.in2.wallet.infrastructure.vault.adapter.hashicorp.model;

import java.util.Map;

/*
    *
    * The object sent to the Hashicorp API v2 needs to be a JSON object with the key "data" and the value being a map of key-value pairs.
    * Further information: https://developer.hashicorp.com/vault/api-docs/secret/kv/kv-v2
    *
 */
public record HashicorpSecretRequest(Map<String, String> data) {
    public HashicorpSecretRequest(String key, Object value) {
        this(Map.of(key, value.toString()));
    }
}
