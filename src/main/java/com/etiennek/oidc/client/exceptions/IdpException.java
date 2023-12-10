package com.etiennek.oidc.client.exceptions;

import java.util.List;
import java.util.Map;

import lombok.Getter;

@Getter
public class IdpException extends RuntimeException {
    private Map<String, List<String>> parameters;

    public IdpException(String message, Map<String, List<String>> parameters) {
        super(message);
        this.parameters = parameters;
    }
}
