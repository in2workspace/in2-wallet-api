package es.in2.wallet.api.crypto;

import es.in2.wallet.api.crypto.service.DidKeyGeneratorService;
import es.in2.wallet.api.crypto.service.KeyGenerationService;
import es.in2.wallet.vault.service.VaultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static es.in2.wallet.api.util.MessageUtils.PROCESS_ID;

@Tag(name = "DID Controller", description = "Endpoints for creating and managing DIDs")
@Slf4j
@RestController
@RequestMapping("/api/v1/dids")
@RequiredArgsConstructor
public class CryptoController {

    private final DidKeyGeneratorService didKeyGeneratorService;
    private final KeyGenerationService keyGenerationService;
    private final VaultService vaultService;

    @PostMapping(path = "/key")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Create DID Key",
            description = "Create a DID Key."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Success",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public Mono<Void> createDidKey(){
        // Create a unique ID for the process
        String processId = UUID.randomUUID().toString();
        MDC.put(PROCESS_ID, processId);
        log.info("ProcessID: {} - Creating did:key...", processId);
        return keyGenerationService.generateES256r1ECKeyPair()
                .flatMap(didKeyGeneratorService::generateDidKeyJwkJcsPubWithFromKeyPair)
                .flatMap(vaultService::saveSecret)
                .doFinally(signalType -> MDC.remove(PROCESS_ID));

    }
}
