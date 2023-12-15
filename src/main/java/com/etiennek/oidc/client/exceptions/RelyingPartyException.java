package com.etiennek.oidc.client.exceptions;

public class RelyingPartyException extends RuntimeException {
    public RelyingPartyException(String message) {
        super(message);
    }

    public RelyingPartyException(String message, Throwable cause) {
        super(message, cause);
    }
}
