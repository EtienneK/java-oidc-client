package com.etiennek.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import com.etiennek.oidc.client.Issuer;

public class Spec {
    @Test
    public void Should_be_able_to_construct_an_Authorization_Endpoint() {

        var issuer = Issuer.builder()
                .authorizationEndpoint("https://www.example.com/authz?extra%26_key=extra_value%26%26%26")
                .tokenEndpoint("https://www.example.com/token")
                .build();

        var client = issuer.clientBuilder()
                .clientId("client_id_12345")
                .clientSecret("client_secret09876")
                .redirectUris(List.of(URI.create("https://example.com/redirect_1"),
                        URI.create("https://example.com/redirect_2")))
                .build();

        var authzUrl = client.authorizationUrlBuilder()
                .scope("read:user")
                .redirectUri(URI.create("https://www.example.com/redirect_3"))
                .state("very_secret_state!")
                .build();

        assertEquals(
                "https://www.example.com/authz?client_id=client_id_12345&extra%26_key=extra_value%26%26%26&redirect_uri=https%3A%2F%2Fwww.example.com%2Fredirect_3&scope=read%3Auser&state=very_secret_state%21",
                authzUrl.toString());
    }

    @Test
    public void github_authz() {

        var issuer = Issuer.builder()
                .authorizationEndpoint("https://github.com/login/oauth/authorize")
                .tokenEndpoint("https://github.com/login/oauth/access_token")
                .build();

        var client = issuer.clientBuilder()
                .clientId("b80900c06c3d011210ee")
                .clientSecret("ed6d8f9aa31a98c5f8539d8e448aaa4eb199faf6")
                .redirectUris(List.of(URI.create("https://boot.local.etkhome.com")))
                .build();

        var authzUrl = client.authorizationUrlBuilder()
                .scope("read:user")
                .state("gjrnu423t43jktn34jktntybh5j")
                .build();

        System.out.println(authzUrl);
    }

    @Test
    public void github_token() throws InterruptedException, ExecutionException {

        var issuer = Issuer.builder()
                .authorizationEndpoint("https://github.com/login/oauth/authorize")
                .tokenEndpoint("https://github.com/login/oauth/access_token")
                .build();

        var client = issuer.clientBuilder()
                .clientId("b80900c06c3d011210ee")
                .clientSecret("ed6d8f9aa31a98c5f8539d8e448aaa4eb199faf6")
                .redirectUris(List.of(URI.create("https://boot.local.etkhome.com")))
                .build();

        var tokens = client.oauthCallback(
                URI.create("https://boot.local.etkhome.com"),
                Map.of("code", List.of("3679df5ac308faeb67d2"),
                        "state", List.of("gjrnu423t43jktn34jktntybh5j")),
                Map.of("state", "gjrnu423t43jktn34jktntybh5j"));

        System.out.println(tokens.get());
    }
}
