package es.in2.wallet.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CredentialFormats {
    JWT_VC(0),
    CWT_VC(1);

    private final int code;

    public static CredentialFormats fromCode(int code) {
        for (CredentialFormats format : values()) {
            if (format.code == code) {
                return format;
            }
        }
        throw new IllegalArgumentException("Invalid code for CredentialFormats: " + code);
    }
}

