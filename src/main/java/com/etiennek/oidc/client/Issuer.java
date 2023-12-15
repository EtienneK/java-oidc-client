package com.etiennek.oidc.client;

import java.net.URL;

import com.etiennek.oidc.client.Client.ClientBuilder;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Issuer {
    private URL authorizationEndpoint;
    private URL tokenEndpoint;
    private URL userinfoEndpoint;

    public ClientBuilder clientBuilder() {
        return Client.builder().issuer(this);
    }

}
