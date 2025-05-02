package es.in2.wallet.infrastructure.vault.model;

public enum VaultProviderEnum {
    HASHICORP("hashicorp");

    private final String providerName;

    VaultProviderEnum(String providerName) {
        this.providerName = providerName;
    }

    @Override
    public String toString() {
        return providerName;
    }
}
