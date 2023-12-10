package com.etiennek.oidc.client;

import java.net.URI;
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

    public ClientBuilder clientBuilder() {
        return Client.builder(this);
    }

    public static class IssuerBuilder {
        public IssuerBuilder authorizationEndpoint(String authorizationEndpoint) {
            try {
                this.authorizationEndpoint = URI.create(authorizationEndpoint).toURL();
                return this;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public IssuerBuilder tokenEndpoint(String tokenEndpoint) {
            try {
                this.tokenEndpoint = URI.create(tokenEndpoint).toURL();
                return this;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
