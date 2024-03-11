package es.in2.wallet.domain.model;

import lombok.Builder;

@Builder
public record GlobalErrorMessage(String title, String message, String path) {

}
