package com.etiennek.oidc.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.etiennek.oidc.client.exceptions.IdpException;
import com.etiennek.oidc.client.exceptions.RelyingPartyException;
import com.etiennek.oidc.client.utils.QueryManipulator;
import com.etiennek.oidc.client.utils.Constants.ContentTypes;
import com.etiennek.oidc.client.utils.Constants.HttpHeaders;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import static com.etiennek.oidc.client.utils.UriUtils.*;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Client {
    private Issuer issuer;

    private String clientId;
    private String clientSecret;
    @Builder.Default
    private String scope = "openid";

    @Builder.Default
    private List<URI> redirectUris = new ArrayList<>();

    @Builder.Default
    private Supplier<HttpClient> httpClientFactory = HttpClient::newHttpClient;

    public AuthorizationUrl.AuthorizationUrlBuilder authorizationUrlBuilder() {
        return AuthorizationUrl.builder().client(this);
    }

    public CompletableFuture<Tokens> oauthCallback(URI redirectUri, Map<String, List<String>> parameters,
            Checks checks) {
        if (checks == null)
            checks = Checks.builder().build();

        if (issuer.getTokenEndpoint() == null) {
            throw new NullPointerException("issuer.tokenEndpoint");
        }

        if (parameters.containsKey("state") && checks.getState() == null) {
            throw new NullPointerException("check.state");
        }

        if (!parameters.containsKey("state") && checks.getState() != null) {
            throw new RelyingPartyException("`response.state` is missing");
        }

        if (!checks.getState().equals(firstVal(parameters, "state"))) {
            throw new RelyingPartyException(String.format("state not equal; expected: [%s] actual: [%s]",
                    checks.getState(), firstVal(parameters, "state")));
        }

        if (parameters.containsKey("error")) {
            throw new IdpException("error from IDP", parameters);
        }

        if (!parameters.containsKey("code")) {
            throw new IdpException("code missing from IDP", parameters);
        }

        var bodyParams = new QueryManipulator();
        bodyParams.put("client_id", clientId);
        bodyParams.put("client_secret", clientSecret);
        bodyParams.put("code", firstVal(parameters, "code"));
        bodyParams.put("redirect_uri", redirectUri.toString());

        var request = HttpRequest.newBuilder(toUri(issuer.getTokenEndpoint()))
                .POST(HttpRequest.BodyPublishers.ofString(bodyParams.toQueryString()))
                .headers(
                        HttpHeaders.ACCEPT, ContentTypes.APPLICATION_JSON,
                        HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_FORM_URLENCODED)
                .build();

        var httpClient = httpClientFactory.get();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    var mapper = new ObjectMapper();
                    try {
                        var tokens = mapper.readValue(response.body(), Tokens.class);
                        if (tokens.getError() != null) { // TODO: Handle errors better
                            throw new IdpException(
                                    String.format(
                                            "received error [%s] from IDP while fetching from token endpoint; response body: [%s]",
                                            tokens.getError(), response.body()),
                                    parameters);
                        }
                        return tokens;
                    } catch (JsonProcessingException e) {
                        throw new RelyingPartyException("unable to deserialize JSON retrieved from token endpoint",
                                e);
                    }
                });

    }

    public CompletableFuture<Map<String, Object>> userinfo(Tokens tokens) {
        if (issuer.getUserinfoEndpoint() == null) {
            throw new NullPointerException("issuer.userinfoEndpoint");
        }

        if (tokens.getAccessToken() == null) {
            throw new NullPointerException("tokens.accessToken");
        }

        try {
            var request = HttpRequest.newBuilder()
                    .uri(issuer.getUserinfoEndpoint().toURI())
                    .GET()
                    .headers(
                            HttpHeaders.ACCEPT, ContentTypes.APPLICATION_JSON,
                            HttpHeaders.AUTHORIZATION, "Bearer " + tokens.getAccessToken())
                    .build();

            var httpClient = httpClientFactory.get();
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        var mapper = new ObjectMapper();
                        try {
                            return mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {
                            });
                        } catch (JsonProcessingException e) {
                            throw new RelyingPartyException(
                                    "unable to deserialize JSON retrieved from userinfo endpoint",
                                    e);
                        }
                    });
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private String firstVal(Map<String, List<String>> parameters, String key) {
        var values = parameters.get(key);
        if (values == null || values.isEmpty())
            return null;
        return values.getFirst();
    }

    @Builder
    @Getter
    public static class Checks {
        private String state;
    }
}
