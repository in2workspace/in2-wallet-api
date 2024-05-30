package es.in2.wallet.domain.service.impl;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import es.in2.wallet.domain.exception.JwtInvalidFormatException;
import es.in2.wallet.domain.exception.ParseErrorException;
import es.in2.wallet.domain.model.UVarInt;
import es.in2.wallet.domain.service.VerifierValidationService;
import io.ipfs.multibase.Base58;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static es.in2.wallet.domain.util.ApplicationConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifierValidationServiceImpl implements VerifierValidationService {
    @Override
    public Mono<String> verifyIssuerOfTheAuthorizationRequest(String processId, String jwtAuthorizationRequest) {
        // Parse the Authorization Request in JWT format
        return parseAuthorizationRequestInJwtFormat(processId, jwtAuthorizationRequest)
                // Extract and verify client_id claim from the Authorization Request
                .flatMap(signedJwt -> validateClientIdClaim(processId, signedJwt))
                .flatMap(signedJwt -> getEcPublicKey(processId, signedJwt)
                        // Verify the Authorization Request
                        .flatMap(publicKey -> verifySignedJwtWithPublicKey(processId, publicKey))
                        .flatMap(jwsVerifier -> checkJWSVerifierResponse(signedJwt, jwsVerifier)
                                .doOnSuccess(v -> log.info("ProcessID: {} - Authorization Request verified successfully", processId))
                        )
                        .onErrorResume(e -> {
                            log.error("Error during the verification of Siop Auth Request on JWS format", e);
                            return Mono.error(new ParseErrorException("Error during the verification of Siop Auth Request on JWS format" + e));
                        }))
                .then(Mono.just(jwtAuthorizationRequest));
    }

    private Mono<SignedJWT> parseAuthorizationRequestInJwtFormat(String processId, String requestToken) {
        return Mono.fromCallable(() -> SignedJWT.parse(requestToken))
                .doOnSuccess(signedJWT -> log.info("ProcessID: {} - Siop Auth Request: {}", processId, signedJWT))
                .onErrorResume(e -> Mono.error(new JwtInvalidFormatException("Error parsing signed JWT " + e)));
    }

    private Mono<SignedJWT> validateClientIdClaim(String processId, SignedJWT signedJWTAuthorizationRequest) {
        Map<String, Object> jsonPayload = signedJWTAuthorizationRequest.getPayload().toJSONObject();
        String iss = jsonPayload.get(ISSUER_TOKEN_PROPERTY_NAME).toString();
        String sub = jsonPayload.get(ISSUER_SUB).toString();
        return Mono.fromCallable(() -> {
                    String authenticationRequestClaim = jsonPayload.get("auth_request").toString();
                    Pattern pattern = Pattern.compile("client_id=([^&]+)");
                    Matcher matcher = pattern.matcher(authenticationRequestClaim);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                    throw new IllegalArgumentException("client_id not found in the auth_request");
                })
                .doOnSuccess(clientId -> log.info("ProcessID: {} - client_id retrieved successfully: {}", processId, clientId))
                .flatMap(clientId -> {
                    if (!clientId.equals(iss) || !iss.equals(sub)) {
                        return Mono.error(new IllegalStateException("iss and sub MUST be the DID of the RP and must correspond to the " +
                                "client_id parameter in the Authorization Request"));
                    } else {
                        return Mono.just(signedJWTAuthorizationRequest);
                    }
                })
                .doOnSuccess(clientId -> log.info("ProcessID: {} - client_id validated successfully: {}", processId, clientId))
                .onErrorResume(e -> Mono.error(new ParseErrorException("Error parsing client_id" + e)));
    }

    private Mono<ECPublicKey> getEcPublicKey(String processId, SignedJWT signedJWTAuthorizationRequest) {
        String kid = signedJWTAuthorizationRequest.getHeader().getKeyID();
        return Mono.fromCallable(() ->
                   decodeDidKey(kid)
                )
                .doOnSuccess(ecPublicKey -> log.info("ProcessID: {} - Public EC Key: {}", processId, ecPublicKey))
                .onErrorResume(e -> Mono.error(new ParseErrorException("Error processing JSON" + e)));
    }

    private ECPublicKey decodeDidKey(String didKey){
        if (!didKey.startsWith(DID_KEY_PREFIX)) {
            throw new IllegalArgumentException("Invalid DID Key format");
        }

        String encodedMultiBase58 = didKey.substring(DID_KEY_PREFIX.length());

        int multiCodecKeyCodeForSecp256r1 = 0x1200;

        byte[] publicKey = decodeRawPublicKeyBytesFromMultibase58String(encodedMultiBase58, multiCodecKeyCodeForSecp256r1);

        return decodeKey(publicKey);
    }

    private byte[] decodeRawPublicKeyBytesFromMultibase58String(String encodedMultiBase58, int code) {
        UVarInt codeVarInt = new UVarInt(code);

        byte[] multiCodeAndRawKey = Base58.decode(encodedMultiBase58);

        return Arrays.copyOfRange(multiCodeAndRawKey, codeVarInt.getLength(), multiCodeAndRawKey.length);
    }

    private ECPublicKey decodeKey(byte[] encoded) {
        ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec("secp256r1");
        ECPublicKeySpec keySpec = new ECPublicKeySpec(params.getCurve().decodePoint(encoded), params);
        return new BCECPublicKey("ECDSA", keySpec, BouncyCastleProvider.CONFIGURATION);
    }

    private Mono<ECDSAVerifier> verifySignedJwtWithPublicKey(String processId, ECPublicKey ecPublicJWK) {
        return Mono.fromCallable(() -> new ECDSAVerifier(ecPublicJWK))
                .doOnSuccess(jwsVerifier -> log.info("ProcessID: {} - JWS Verifier generated successfully: {}", processId, jwsVerifier))
                .onErrorResume(e -> Mono.error(new ParseErrorException("Error verifying Jwt with Public EcKey " + e)));
    }

    private Mono<Void> checkJWSVerifierResponse(SignedJWT signedJWTResponse, JWSVerifier verifier) {
        try {
            if (!signedJWTResponse.verify(verifier)) {
                return Mono.error(new JwtInvalidFormatException("The 'request_token' is not valid"));
            } else {
                return Mono.empty();
            }
        } catch (Exception e) {
            return Mono.error(new ParseErrorException("Error verifying Jwt with Public EcKey" + e));
        }
    }
}
