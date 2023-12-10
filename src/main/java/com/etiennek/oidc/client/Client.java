package com.etiennek.oidc.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.etiennek.oidc.client.exceptions.IdpException;
import com.etiennek.oidc.client.exceptions.RelyingPartyException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(builderMethodName = "internalBuilder")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Client {
    private Issuer issuer;

    private String clientId;
    private String clientSecret;

    @Builder.Default
    private List<URI> redirectUris = new ArrayList<>();

    static ClientBuilder builder(Issuer issuer) {
        return internalBuilder().issuer(issuer);
    }

    public AuthorizationUrl.AuthorizationUrlBuilder authorizationUrlBuilder() {
        return AuthorizationUrl.builder(this);
    }

    public CompletableFuture<String> oauthCallback(URI redirectUri, Map<String, List<String>> parameters) {
        return oauthCallback(redirectUri, parameters, null);
    }

    public CompletableFuture<String> oauthCallback(URI redirectUri, Map<String, List<String>> parameters,
            Map<String, String> checks) {
        if (checks == null)
            checks = new HashMap<>();

        if (parameters.containsKey("state") && !checks.containsKey("state")) {
            throw new IllegalArgumentException("check.state is missing");
        }

        if (!parameters.containsKey("state") && checks.containsKey("state")) {
            throw new RelyingPartyException("`response.state` is missing");
        }

        if (!firstVal(parameters, "state").equals(checks.get("state"))) {
            throw new RelyingPartyException(String.format("state not equal; expected: [%s] actual: [%s]",
                    checks.get("state"), firstVal(parameters, "state")));
        }

        if (parameters.containsKey("error")) {
            throw new IdpException("error from IDP", parameters);
        }

        if (!parameters.containsKey("code")) {
            throw new IdpException("code missing from IDP", parameters);
        }

        try {
            var bodyParams = new HashMap<String, String>();
            bodyParams.put("client_id", clientId);
            bodyParams.put("client_secret", clientSecret);
            bodyParams.put("code", firstVal(parameters, "code"));
            bodyParams.put("redirect_uri", redirectUri.toString());

            var form = bodyParams.entrySet()
                    .stream()
                    .filter(e -> e.getValue() != null)
                    .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            var request = HttpRequest.newBuilder(issuer.getTokenEndpoint().toURI())

                    .headers(
                            "Content-Type", "application/x-www-form-urlencoded",
                            "Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            var httpClient = HttpClient.newHttpClient();
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        return response.body();
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
}
