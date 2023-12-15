package com.etiennek.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.etiennek.oidc.client.AuthorizationUrl;
import com.etiennek.oidc.client.Client;
import com.etiennek.oidc.client.Issuer;
import com.etiennek.oidc.client.Tokens;
import com.etiennek.oidc.client.Client.Checks;

import lombok.RequiredArgsConstructor;

import static com.etiennek.oidc.client.utils.UriUtils.*;

@ExtendWith(MockitoExtension.class)
public class Spec {
    private static final TestConstants TS = TestConstants.INSTANCE;

    @Mock
    HttpResponse<String> mockResponse;

    @Spy
    HttpClient httpClient;

    @Captor
    ArgumentCaptor<HttpRequest> requestCaptor;

    @Test
    public void Should_be_able_to_construct_an_Authorization_Endpoint_using_Authorization_URL_Request_arguments()
            throws MalformedURLException {
        var issuer = TS.issuerBuilder().build();
        var client = TS.clientBuilder(issuer).build();
        var authzUrl = TS.authorizationUrlBuilder(client).build();

        assertEquals(
                "https://www.example.com/authz?client_id="
                        + urlEncode(TS.CLIENT_ID)
                        + "&extra%26_key=extra_value%26%26%26&redirect_uri="
                        + urlEncode(TS.REDIRECT_URI_STR_3)
                        + "&scope="
                        + urlEncode(TS.AUTHZ_URL_SCOPE)
                        + "&state="
                        + urlEncode(TS.STATE),
                authzUrl.toString());
    }

    @Test
    public void Should_be_able_to_construct_an_Authorization_Endpoint_using_Client_arguments_which_includes_single_redirect_URI()
            throws MalformedURLException {
        var issuer = TS.issuerBuilder().build();
        var client = TS.clientBuilder(issuer)
                .redirectUris(List.of(TS.REDIRECT_URI_1))
                .build();
        var authzUrl = TS.authorizationUrlBuilder(client)
                .scope(null)
                .redirectUri(null)
                .build();

        assertEquals(
                "https://www.example.com/authz?client_id="
                        + urlEncode(TS.CLIENT_ID)
                        + "&extra%26_key=extra_value%26%26%26&redirect_uri="
                        + urlEncode(TS.REDIRECT_URI_STR_1)
                        + "&scope="
                        + urlEncode(TS.CLIENT_SCOPE)
                        + "&state="
                        + urlEncode(TS.STATE),
                authzUrl.toString());
    }

    @Test
    public void Should_be_able_to_construct_an_Authorization_Endpoint_using_Client_arguments_which_includes_multiple_redirect_URIs()
            throws MalformedURLException {
        var issuer = TS.issuerBuilder().build();
        var client = TS.clientBuilder(issuer)
                .build();
        var authzUrl = TS.authorizationUrlBuilder(client)
                .scope(null)
                .redirectUri(null)
                .build();

        assertEquals(
                "https://www.example.com/authz?client_id="
                        + urlEncode(TS.CLIENT_ID)
                        + "&extra%26_key=extra_value%26%26%26"
                        + "&scope="
                        + urlEncode(TS.CLIENT_SCOPE)
                        + "&state="
                        + urlEncode(TS.STATE),
                authzUrl.toString());
    }

    @Test
    public void Should_be_able_to_retrieve_and_parse_Tokens()
            throws InterruptedException, ExecutionException, MalformedURLException {
        when(mockResponse.body()).thenReturn("""
                {
                    "access_token":"access_token_123456",
                    "scope":"read:user",
                    "token_type":"bearer"
                }""");
        var completableFuture = CompletableFuture.completedFuture(mockResponse);
        when(httpClient.<String>sendAsync(requestCaptor.capture(), any())).thenReturn(completableFuture);

        var issuer = TS.issuerBuilder().build();
        var client = TS.clientBuilder(issuer).httpClientFactory(() -> httpClient).build();

        var tokens = client.oauthCallback(
                URI.create("https://www.example.com/redirect_3"),
                Map.of("code", List.of("code_from_idp_123456"),
                        "state", List.of("state_12345678")),
                Checks.builder().state("state_12345678").build());

        var body = new HttpRequestBody(requestCaptor.getValue()).get();
        assertEquals(
                "client_id=client_id_128974329&client_secret=client_SECr37_8972%23%24%401%40%2F%5C128974329&code=code_from_idp_123456&redirect_uri=https%3A%2F%2Fwww.example.com%2Fredirect_3",
                body);
        assertEquals(TS.TOKEN_URI, requestCaptor.getValue().uri());
        assertEquals("POST", requestCaptor.getValue().method());
        assertEquals(
                Tokens.builder()
                        .accessToken("access_token_123456")
                        .scope("read:user")
                        .tokenType("bearer").build(),
                tokens.get());
    }

    @RequiredArgsConstructor
    public static class HttpRequestBody {
        private final HttpRequest request;

        static final class StringSubscriber implements Flow.Subscriber<ByteBuffer> {
            final BodySubscriber<String> wrapped;

            StringSubscriber(BodySubscriber<String> wrapped) {
                this.wrapped = wrapped;
            }

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                wrapped.onSubscribe(subscription);
            }

            @Override
            public void onNext(ByteBuffer item) {
                wrapped.onNext(List.of(item));
            }

            @Override
            public void onError(Throwable throwable) {
                wrapped.onError(throwable);
            }

            @Override
            public void onComplete() {
                wrapped.onComplete();
            }
        }

        public String get() {
            String body = request.bodyPublisher().map(p -> {
                var bodySubscriber = BodySubscribers.ofString(StandardCharsets.UTF_8);
                var flowSubscriber = new StringSubscriber(bodySubscriber);
                p.subscribe(flowSubscriber);
                return bodySubscriber.getBody().toCompletableFuture().join();
            }).get();
            return body;
        }

    }

    public static class TestConstants {
        public static final TestConstants INSTANCE = new TestConstants();

        // Issuer
        public final String AUTHZ_URI_STR = "https://www.example.com/authz?extra%26_key=extra_value%26%26%26";
        public final String TOKEN_URI_STR = "https://www.example.com/token?extra%26_key=extra_value%26%26%26&something_else=yes";
        public final String USERINFO_URI_STR = "https://www.example.com/userinfo?extra%26_key=extra_value%26%26%26&yes=no";

        public final URI AUTHZ_URI = URI.create(AUTHZ_URI_STR);
        public final URI TOKEN_URI = URI.create(TOKEN_URI_STR);
        public final URI USERINFO_URI = URI.create(USERINFO_URI_STR);

        public final URL AUTHZ_URL;
        public final URL TOKEN_URL;
        public final URL USERINFO_URL;

        // Client
        public final String CLIENT_ID = "client_id_128974329";
        public final String CLIENT_SECRET = "client_SECr37_8972#$@1@/\\128974329";
        public final String REDIRECT_URI_STR_1 = "https://client.redirect.uri1.example/with_path/?and_some_query=q1&q2=val";
        public final URI REDIRECT_URI_1 = URI.create(REDIRECT_URI_STR_1);
        public final String REDIRECT_URI_STR_2 = "https://client.redirect.uri2.local/with_path/?and_some_query=q1&q2=val";
        public final URI REDIRECT_URI_2 = URI.create(REDIRECT_URI_STR_2);
        public final String CLIENT_SCOPE = "this is a scope string";

        // Authz Request URL
        public final String REDIRECT_URI_STR_3 = "https://client.redirect.uri3.example.com/with_path/?and_some_query=q1&q2=val";
        public final URI REDIRECT_URI_3 = URI.create(REDIRECT_URI_STR_3);
        public final String AUTHZ_URL_SCOPE = "and this is another scope string";
        public final String STATE = "few89f4nkjb234#@$%^%&*@!@#$%^&*()\\/+_)(][{}4238943ujkn43";

        private TestConstants() {
            try {
                AUTHZ_URL = AUTHZ_URI.toURL();
                TOKEN_URL = TOKEN_URI.toURL();
                USERINFO_URL = USERINFO_URI.toURL();
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public final Issuer.IssuerBuilder issuerBuilder() {
            return Issuer.builder()
                    .authorizationEndpoint(AUTHZ_URL)
                    .tokenEndpoint(TOKEN_URL)
                    .userinfoEndpoint(USERINFO_URL);
        }

        public final Client.ClientBuilder clientBuilder(Issuer issuer) {
            return issuer.clientBuilder()
                    .clientId(CLIENT_ID)
                    .clientSecret(CLIENT_SECRET)
                    .redirectUris(List.of(REDIRECT_URI_1, REDIRECT_URI_2))
                    .scope(CLIENT_SCOPE);
        }

        public final AuthorizationUrl.AuthorizationUrlBuilder authorizationUrlBuilder(Client client) {
            return client.authorizationUrlBuilder()
                    .redirectUri(REDIRECT_URI_3)
                    .scope(AUTHZ_URL_SCOPE)
                    .state(STATE);
        }
    }
}
