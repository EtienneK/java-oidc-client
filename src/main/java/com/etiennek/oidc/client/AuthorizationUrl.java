package com.etiennek.oidc.client;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(builderMethodName = "internalBuilder")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthorizationUrl {
    private Client client;
    private String scope;
    private URI redirectUri;
    private String state;

    static AuthorizationUrlBuilder builder(Client client) {
        return internalBuilder().client(client);
    }

    public static class AuthorizationUrlBuilder {
        private String getRedirectUriString() {
            if (redirectUri != null)
                return redirectUri.toString();
            final var redirectUris = client.getRedirectUris();
            if (redirectUris == null || redirectUris.isEmpty())
                return null;
            if (redirectUris.size() == 1)
                return redirectUris.get(0).toString();
            return null;
        }

        public URL build() {
            final var qm = new QueryManipulator(this.client.getIssuer().getAuthorizationEndpoint());
            qm.set("client_id", client.getClientId());
            qm.set("scope", scope == null ? "openid" : scope);
            qm.set("redirect_uri", getRedirectUriString());
            qm.set("state", state);
            return qm.toUrl();
        }
    }

    static class QueryManipulator {
        private URL original;
        private Map<String, List<String>> query = new HashMap<>();

        public QueryManipulator(URL url) {
            try {
                this.original = url;
                var q = url.getQuery();
                if (q == null || q.trim().length() == 0) {
                    return;
                }

                var querySplit = q.split("&");
                for (var keyVal : querySplit) {
                    var keyValSplit = keyVal.split("=", 2);
                    var key = URLDecoder.decode(keyValSplit[0], StandardCharsets.UTF_8.name());
                    var value = keyValSplit.length == 2
                            ? URLDecoder.decode(keyValSplit[1], StandardCharsets.UTF_8.name())
                            : "";
                    add(key, value);
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        public void add(String key, String value) {
            if (key == null || value == null)
                return;
            if (!query.containsKey(key)) {
                query.put(key, new ArrayList<>());
            }
            query.get(key).add(value);
        }

        public void set(String key, String value) {
            if (key == null || value == null)
                return;
            var arr = new ArrayList<String>();
            arr.add(value);
            query.put(key, arr);
        }

        public void remove(String key) {
            if (key == null)
                return;
            query.remove(key);
        }

        public URL toUrl() {
            var queryString = query.entrySet().stream()
                    .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
                    .map(entry -> {
                        var key = entry.getKey();
                        return entry.getValue().stream()
                                .map(val -> encode(key) + "=" + encode(val))
                                .collect(Collectors.joining("&"));
                    }).collect(Collectors.joining("&"));

            try {
                var url = new URI(original.getProtocol(), original.getUserInfo(), original.getHost(),
                        original.getPort(),
                        original.getPath(), null, null).toURL();
                if (queryString == null || queryString.trim().length() == 0)
                    return url;
                else
                    return URI.create(url + "?" + queryString).toURL();
            } catch (URISyntaxException | MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        private String encode(String toEncode) {
            try {
                return URLEncoder.encode(toEncode, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
