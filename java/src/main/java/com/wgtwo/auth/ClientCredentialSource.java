package com.wgtwo.auth;

import com.wgtwo.auth.model.Token;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

/**
 * Returns an up-to-date client credentials token.
 * <p>
 * Will replace the cached token if expired or will expire within two minutes.
 */
public class ClientCredentialSource {
    private final static Duration REFRESH_TOKEN_PERIOD = Duration.ofMinutes(2);
    private final Clock clock;
    private final Supplier<Token> tokenSupplier;

    private volatile Token cached = new Token("", "", Instant.MIN, "");

    public ClientCredentialSource(@NotNull Clock clock, @NotNull Supplier<Token> tokenSupplier) {
        this.clock = clock;
        this.tokenSupplier = tokenSupplier;
    }

    @NotNull
    public synchronized Token fetchToken() {
        if (isExpired(cached)) {
            cached = tokenSupplier.get();
        }
        return cached;
    }

    public BearerTokenCallCredentials callCredentials() {
        return new BearerTokenCallCredentials(fetchToken()::getAccessToken);
    }

    private boolean isExpired(Token token) {
        Instant now = clock.instant();
        Instant expiry = token.getExpiry();
        return now.plus(REFRESH_TOKEN_PERIOD).isAfter(expiry);
    }
}
