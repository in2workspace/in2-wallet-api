package es.in2.wallet.vault.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.api.exception.ParseErrorException;
import es.in2.wallet.vault.domain.VaultSecretData;
import es.in2.wallet.vault.service.GenericVaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.ReactiveVaultOperations;
import reactor.core.publisher.Mono;

import javax.security.auth.login.CredentialNotFoundException;

import java.util.Map;

import static es.in2.wallet.api.util.MessageUtils.*;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vault.secret-provider.name", havingValue = "hashicorp")
public class HashicorpAdapter implements GenericVaultService {
    private final ObjectMapper objectMapper;
    private final ReactiveVaultOperations vaultOperations;
    @Override
    public Mono<Void> saveSecret(Map<String, String> secrets) {
        String processId = MDC.get(PROCESS_ID);
        return vaultOperations.write("kv/" + secrets.get("did"),
                        VaultSecretData.builder()
                                .privateKey(secrets.get(PRIVATE_KEY_TYPE))
                                .publicKey(secrets.get(PUBLIC_KEY_TYPE))
                                .build())
                .doOnSuccess(voidValue -> log.debug("ProcessID: {} - Secret saved successfully", processId))
                .doOnError(error -> log.error("ProcessID: {} - Error saving secret: {}", processId, error.getMessage(), error))
                .then();
    }
    @Override
    public Mono<String> getSecretByKey(String key, String type) {
        String processId = MDC.get(PROCESS_ID);
        return vaultOperations.read("kv/" + key)
                .flatMap(response -> {
                    try {
                        if (response.getData() != null) {
                            String json = objectMapper.writeValueAsString(response.getData());
                            VaultSecretData secret = objectMapper.readValue(json, VaultSecretData.class);
                            if (PRIVATE_KEY_TYPE.equals(type)) {
                                return Mono.just(secret.privateKey());
                            } else if (PUBLIC_KEY_TYPE.equals(type)) {
                                return Mono.just(secret.publicKey());
                            } else {
                                return Mono.error(new IllegalStateException("Invalid type"));
                            }
                        } else {
                            return Mono.error(new CredentialNotFoundException("Secret not found"));
                        }
                    } catch (Exception e) {
                        return Mono.error(new ParseErrorException("Vault response could not be parsed"));
                    }
                })
                .doOnSuccess(voidValue -> log.debug("ProcessID: {} - Secret retrieved successfully", processId))
                .doOnError(error -> log.error("Error retrieving secret: {}", error.getMessage(), error));
    }

    @Override
    public Mono<Void> deleteSecretByKey(String key) {
        String processId = MDC.get(PROCESS_ID);
        return vaultOperations.delete("kv/" + key)
                .doOnSuccess(voidValue -> log.debug("ProcessID: {} - Secret deleted successfully", processId))
                .onErrorResume(Exception.class, e -> {
                    log.error("ProcessID: {} - Error deleting secret: {}", processId, e.getMessage(), e);
                    return Mono.error(e);
                });
    }
}
