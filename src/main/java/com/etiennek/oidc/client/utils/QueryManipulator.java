package com.etiennek.oidc.client.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.etiennek.oidc.client.utils.UriUtils.*;

public class QueryManipulator {
    private Map<String, List<String>> query = new HashMap<>();

    public QueryManipulator() {
    }

    public QueryManipulator(final String queryString) {
        if (queryString == null || queryString.trim().length() == 0) {
            return;
        }

        var q = queryString.trim();
        if (q.startsWith("?")) {
            q = q.replaceFirst("\\?", "");
        }

        if (q.trim().length() == 0) {
            return;
        }

        final var querySplit = q.split("&");
        for (var keyVal : querySplit) {
            final var keyValSplit = keyVal.split("=", 2);
            final var key = urlDecode(keyValSplit[0]);
            final var value = keyValSplit.length == 2 ? urlDecode(keyValSplit[1]) : "";
            add(key, value);
        }
    }

    public QueryManipulator(final URL fromUrl) {
        this(fromUrl.getQuery());
    }

    public void add(final String key, final String value) {
        if (key == null || value == null) {
            return;
        }

        if (!query.containsKey(key)) {
            query.put(key, new ArrayList<>());
        }
        query.get(key).add(value);
    }

    public void put(final String key, final String value) {
        if (key == null || value == null) {
            return;
        }

        final var arr = new ArrayList<String>();
        arr.add(value);
        query.put(key, arr);
    }

    public void remove(final String key) {
        if (key == null)
            return;
        query.remove(key);
    }

    public String toQueryString() {
        return query.entrySet().stream()
                .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
                .map(entry -> {
                    var key = entry.getKey();
                    return entry.getValue().stream()
                            .sorted((a, b) -> a.compareTo(b))
                            .map(val -> urlEncode(key) + "=" + urlEncode(val))
                            .collect(Collectors.joining("&"));
                }).collect(Collectors.joining("&"));
    }

    public URL newUrlWithReplacedQueryString(final URL from) {
        final var queryString = toQueryString();
        try {
            var url = new URI(from.getProtocol(), from.getUserInfo(), from.getHost(),
                    from.getPort(),
                    from.getPath(), null, null).toURL();
            if (queryString == null || queryString.trim().length() == 0)
                return url;
            else
                return URI.create(url + "?" + queryString).toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
