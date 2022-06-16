package com.wgtwo.auth;

import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuth2AccessTokenErrorResponse;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.wgtwo.auth.model.AccessTokenException;
import com.wgtwo.auth.model.Metadata;
import com.wgtwo.auth.model.Token;
import java.io.Closeable;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WgtwoAuth implements Closeable {
    private final OAuth20Service service;

    private final Clock clock;

    public final ClientCredentials clientCredentials = new ClientCredentials();
    public final AuthorizationCode authorizationCode = new AuthorizationCode();


    private WgtwoAuth(String clientId, String clientSecret, String callback, Clock clock, String hostname, Boolean debug) {
        ServiceBuilder builder = new ServiceBuilder(clientId)
                .apiSecret(clientSecret)
                .callback(callback);
        if (debug) {
            builder = builder.debug();
        }
        this.service = builder.build(new WgtwoApi(hostname));
        this.clock = clock;
    }

    @NotNull
    public static WgtwoAuthBuilder builder(@NotNull String clientId, @NotNull String clientSecret) {
        return new WgtwoAuthBuilder(clientId, clientSecret);
    }

    public ClientCredentialSource clientCredentialSource(String scope) {
        return new ClientCredentialSource(clock, () -> clientCredentials.accessToken(scope));
    }

    @Override
    public void close() throws IOException {
        service.close();
    }

    public static class WgtwoAuthBuilder {
        final String clientId;
        final String clientSecret;
        String callback;
        Clock clock = Clock.systemUTC();
        boolean debug = false;
        String hostname = "https://id.wgtwo.com";

        private WgtwoAuthBuilder(String clientId, String clientSecret) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
        }

        @NotNull
        public WgtwoAuthBuilder callbackUri(@Nullable String callbackUri) {
            this.callback = callbackUri;
            return this;
        }

        @NotNull
        public WgtwoAuthBuilder clock(@NotNull Clock clock) {
            this.clock = clock;
            return this;
        }

        @NotNull
        public WgtwoAuthBuilder debug() {
            this.debug = true;
            return this;
        }

        @NotNull
        public WgtwoAuthBuilder oauthServer(@NotNull String hostname) {
            this.hostname = hostname;
            return this;
        }

        @NotNull
        public WgtwoAuth build() {
            return new WgtwoAuth(clientId, clientSecret, callback, clock, hostname, debug);
        }
    }

    public class ClientCredentials {
        public Token accessToken(@Nullable String scope) throws AccessTokenException {
            try {
                OAuth2AccessToken response = service.getAccessTokenClientCredentialsGrant(scope);
                Instant expiry = clock.instant().plusSeconds(response.getExpiresIn());
                return new Token(response.getAccessToken(), "", expiry, response.getScope());
            } catch (OAuth2AccessTokenErrorResponse e) {
                throw new AccessTokenException(e.getError().getErrorString(), e.getErrorDescription());
            } catch (IOException | ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public class AuthorizationCode {
        public String authorizationUrl(
                @NotNull String scope,
                @NotNull String nonce,
                @NotNull String state,
                @NotNull Prompt prompt
        ) {
            Map<String, String> params = new HashMap<>();
            params.put("nonce", nonce);
            if (prompt != Prompt.DEFAULT) {
                params.put("prompt", prompt.value);
            }
            return service.createAuthorizationUrlBuilder()
                    .state(state)
                    .scope(scope)
                    .additionalParams(params)
                    .build();
        }

        public Token accessToken(@NotNull String code) {
            try {
                Instant now = clock.instant();
                OpenIdOAuth2AccessToken response = (OpenIdOAuth2AccessToken) service.getAccessToken(code);
                String openIdToken = response.getOpenIdToken();
                Metadata metadata;
                if (openIdToken != null) {
                    metadata = new Metadata((openIdToken));
                } else {
                    metadata = null;
                }
                Instant expiry = now.plusSeconds(response.getExpiresIn());
                return new Token(response.getAccessToken(), "", expiry, response.getScope(), metadata);
            } catch (OAuth2AccessTokenErrorResponse e) {
                throw new AccessTokenException(e.getError().getErrorString(), e.getErrorDescription());
            } catch (IOException | ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
