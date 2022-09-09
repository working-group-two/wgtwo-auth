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
        public WgtwoAuthBuilder enableDebug() {
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

        @NotNull
        public Token fetchToken(@Nullable String scope) throws AccessTokenException {
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

        public ClientCredentialSource newTokenSource(@Nullable String scope) {
            return new ClientCredentialSource(clock, () -> fetchToken(scope));
        }
    }

    public class AuthorizationCode {
        public String createAuthorizationUrl(
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

        @NotNull
        public Token fetchToken(@NotNull String code) {
            Instant now = clock.instant();
            try {
                OpenIdOAuth2AccessToken response = (OpenIdOAuth2AccessToken) service.getAccessToken(code);
                return createToken(now, response);
            } catch (OAuth2AccessTokenErrorResponse e) {
                throw new AccessTokenException(e.getError().getErrorString(), e.getErrorDescription());
            } catch (IOException | ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public Token refreshToken(@NotNull String refreshToken) {
            Instant now = clock.instant();
            try {
                OpenIdOAuth2AccessToken response = (OpenIdOAuth2AccessToken) service.refreshAccessToken(refreshToken);
                return createToken(now, response);
            } catch (OAuth2AccessTokenErrorResponse e) {
                throw new AccessTokenException(e.getError().getErrorString(), e.getErrorDescription());
            } catch (IOException | ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        private Token createToken(Instant now, OpenIdOAuth2AccessToken token) {
            String openIdToken = token.getOpenIdToken();
            Metadata metadata;
            if (openIdToken != null) {
                metadata = new Metadata((openIdToken));
            } else {
                metadata = null;
            }
            Instant expiry = now.plusSeconds(token.getExpiresIn());
            return new Token(token.getAccessToken(), token.getRefreshToken(), expiry, token.getScope(), metadata);
        }
    }
}
