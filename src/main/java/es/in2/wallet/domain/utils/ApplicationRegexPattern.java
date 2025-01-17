package es.in2.wallet.domain.utils;

import java.util.regex.Pattern;

public class ApplicationRegexPattern {
    private ApplicationRegexPattern() {
        throw new IllegalStateException("Utility class");
    }
    public static final Pattern PROOF_DOCUMENT_PATTERN = Pattern.compile("proof");
    public static final Pattern VP_DOCUMENT_PATTERN = Pattern.compile("vp");
    public static final Pattern JWT_TYPE = Pattern.compile("JWT");
    public static final Pattern LOGIN_REQUEST_PATTERN = Pattern.compile("(https|http)\\S*(authentication-request|authentication-requests)\\S*");
    public static final Pattern CREDENTIAL_OFFER_PATTERN = Pattern.compile("(https|http)\\S*(credential-offer)\\S*");
    public static final Pattern OPENID_CREDENTIAL_OFFER_PATTERN = Pattern.compile("openid-credential-offer://\\S*");
    public static final Pattern EBSI_CREDENTIAL_OFFER_PATTERN = Pattern.compile("\\S*(conformance.ebsi)\\S*");
    public static final Pattern VP_TOKEN_AUTHENTICATION_REQUEST_PATTERN = Pattern.compile("\\S*(response_type=vp_token)\\S*");
    public static final Pattern OPENID_VP_TOKEN_AUTHENTICATION_REQUEST_PATTERN = Pattern.compile("openid4vp://\\S*");
}
