package es.in2.wallet.domain.services.impl;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import es.in2.wallet.domain.exceptions.JwtInvalidFormatException;
import es.in2.wallet.domain.exceptions.ParseErrorException;
import es.in2.wallet.application.dto.UVarInt;
import es.in2.wallet.domain.services.VerifierValidationService;
import es.in2.wallet.infrastructure.appconfiguration.exception.ClientIdMismatchException;
import es.in2.wallet.infrastructure.appconfiguration.exception.InvalidRequestException;
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

import static es.in2.wallet.domain.utils.ApplicationConstants.DID_KEY_PREFIX;
import static es.in2.wallet.domain.utils.ApplicationConstants.JWT_ISS_CLAIM;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifierValidationServiceImpl implements VerifierValidationService {

    @Override
    public Mono<String> verifyIssuerOfTheAuthorizationRequest(String processId, String jwtAuthorizationRequest) {
        // Parse the Authorization Request in JWT format
        return parseAuthorizationRequestInJwtFormat(processId, jwtAuthorizationRequest)
                // Extract and verify client_id claim from the Authorization Request
                .flatMap(signedJwt -> checkJwtClaims(processId, signedJwt))
                .flatMap(signedJwt -> validateVerifierClaims(processId, signedJwt))
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

    private Mono<SignedJWT> checkJwtClaims(String processId, SignedJWT signedJWTAuthorizationRequest) {
        Map<String, Object> claimsHeader  = signedJWTAuthorizationRequest.getHeader().toJSONObject();
        log.info("ProcessID: {} - JWT Header content: {}", processId, claimsHeader);
        Object typClaim = claimsHeader.get("typ");
        if (typClaim == null || !"oauth-authz-req+jwt".equals(typClaim.toString())) {
            String errorMessage = "Invalid or missing 'typ' claim in Authorization Request. Expected: oauth-authz-req+jwt";
            log.warn("ProcessID: {} - {}", processId, errorMessage);
            return Mono.error(new IllegalArgumentException(errorMessage));
        }
        /*
        Map<String, Object> claimsPayload = signedJWTAuthorizationRequest.getPayload().toJSONObject();
        if (!claimsPayload.containsKey("dcql_query")) {
            log.warn("ProcessID: {} - Missing dcql_query parameter", processId);
            return Mono.error(new InvalidRequestException("Authorization Request must include either 'dcql_query'"));
        }
        */
        return Mono.just(signedJWTAuthorizationRequest);
    }


    private Mono<SignedJWT> parseAuthorizationRequestInJwtFormat(String processId, String requestToken) {
        return Mono.fromCallable(() -> SignedJWT.parse(requestToken))
                .doOnSuccess(signedJWT -> log.info("ProcessID: {} - Siop Auth Request: {}", processId, signedJWT))
                .onErrorResume(e -> Mono.error(new JwtInvalidFormatException("Error parsing signed JWT " + e)));
    }

    private Mono<SignedJWT> validateVerifierClaims(String processId, SignedJWT signedJWTAuthorizationRequest) {
        Map<String, Object> jsonPayload = signedJWTAuthorizationRequest.getPayload().toJSONObject();
        String iss = jsonPayload.get(JWT_ISS_CLAIM).toString();
        String clientId = (String) jsonPayload.get("client_id");
        return Mono.fromCallable(() -> {
                    if (clientId == null || clientId.isEmpty()) {
                        throw new ClientIdMismatchException("client_id not found in the auth_request");
                    }
                    return clientId;
                })
                .doOnSuccess(id -> log.info("ProcessID: {} - client_id retrieved successfully: {}", processId, id))
                .flatMap(id -> {
                    if (!id.equals(iss)) {
                        return Mono.error(new ClientIdMismatchException("iss and sub MUST be the DID of the RP and must correspond to the client_id parameter in the Authorization Request"));
                    } else {
                        return Mono.just(signedJWTAuthorizationRequest);
                    }
                })
                .doOnSuccess(id -> log.info("ProcessID: {} - client_id and scope validated successfully: {}", processId, id))
                .onErrorResume(e -> Mono.error(new ParseErrorException("Error parsing client_id or validating scope: " + e)));
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
