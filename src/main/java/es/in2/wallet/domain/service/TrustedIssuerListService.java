package es.in2.wallet.domain.service;

import es.in2.wallet.domain.model.IssuerCredentialsCapabilities;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TrustedIssuerListService {
    Mono<List<IssuerCredentialsCapabilities>> getTrustedIssuerListData(String id);
}
