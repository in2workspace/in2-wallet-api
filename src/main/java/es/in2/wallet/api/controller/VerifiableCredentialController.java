package es.in2.wallet.api.controller;


import es.in2.wallet.api.facade.UserDataFacadeService;
import es.in2.wallet.api.model.CredentialsBasicInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static es.in2.wallet.api.util.ApplicationUtils.getCleanBearerAndUserIdFromToken;

@RestController
@RequestMapping("/api/v2/credentials")
@Slf4j
@RequiredArgsConstructor
public class VerifiableCredentialController {

    private final UserDataFacadeService userDataFacadeService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "List Verifiable Credentials",
            description = "Retrieve a list of Verifiable Credentials",
            tags = {"Verifiable Credential Management"}
    )
    @ApiResponse(responseCode = "200", description = "Verifiable credentials retrieved successfully.")
    @ApiResponse(responseCode = "400", description = "Invalid request.")
    @ApiResponse(responseCode = "500", description = "Internal server error.")
    public Mono<List<CredentialsBasicInfo>> getVerifiableCredentialList(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        log.debug("VerifiableCredentialController.getVerifiableCredential()");

        String processId = UUID.randomUUID().toString();

        MDC.put("processId", processId);
        return getCleanBearerAndUserIdFromToken(authorizationHeader)
                .flatMap(userId -> userDataFacadeService.getUserVCs(processId,userId));
    }
    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Delete Verifiable Credential",
            description = "Delete the verifiable credential from the context broker.",
            tags = {"Verifiable Credential Management"}
    )
    @ApiResponse(responseCode = "200", description = "Verifiable credential deleted successfully.")
    @ApiResponse(responseCode = "400", description = "Invalid request.")
    @ApiResponse(responseCode = "404", description = "Verifiable credential not found")
    @ApiResponse(responseCode = "500", description = "Internal server error.")
    public Mono<Void> deleteVerifiableCredential(@RequestParam String credentialId, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        log.debug("VerifiableCredentialController.deleteVerifiableCredential()");

        String processId = UUID.randomUUID().toString();

        MDC.put("processId", processId);

        return getCleanBearerAndUserIdFromToken(authorizationHeader)
                .flatMap(userId -> userDataFacadeService.deleteVerifiableCredentialById(processId,credentialId, userId))
                .then();
    }

}

