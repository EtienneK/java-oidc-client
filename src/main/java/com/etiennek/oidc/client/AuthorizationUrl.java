package com.etiennek.oidc.client;

import java.net.URI;
import java.net.URL;
import com.etiennek.oidc.client.utils.QueryManipulator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthorizationUrl {
    private Client client;
    private String scope;
    private URI redirectUri;
    private String state;

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
            qm.put("client_id", client.getClientId());
            qm.put("scope", scope == null ? client.getScope() : scope);
            qm.put("redirect_uri", getRedirectUriString());
            qm.put("state", state);
            return qm.newUrlWithReplacedQueryString(this.client.getIssuer().getAuthorizationEndpoint());
        }
    }
}
