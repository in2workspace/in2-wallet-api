package es.in2.wallet.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CredentialStatus {
    VALID(1),
    ISSUED(2),
    REVOKED(3),
    EXPIRED(4);

    private final int code;

    public static CredentialStatus fromCode(int code) {
        for (CredentialStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid code for CredentialStatus: " + code);
    }
}


