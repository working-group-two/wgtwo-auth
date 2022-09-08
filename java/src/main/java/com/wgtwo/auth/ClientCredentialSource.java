package com.wgtwo.auth;

import com.wgtwo.auth.model.Token;
import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

/**
 * Returns an up-to-date client credentials token.
 * <p>
 * Will replace the cached token if expired or will expire within two minutes.
 */
public class ClientCredentialSource {
    private final Clock clock;
    private final Supplier<Token> tokenSupplier;

    private volatile Token cached = new Token("", "", Instant.MIN, "");

    public ClientCredentialSource(@NotNull Clock clock, @NotNull Supplier<Token> tokenSupplier) {
        this.clock = clock;
        this.tokenSupplier = tokenSupplier;
    }

    @NotNull
    public synchronized Token token() {
        if (isExpired(cached)) {
            cached = tokenSupplier.get();
        }
        return cached;
    }

    public BearerTokenCallCredentials callCredentials() {
        return new BearerTokenCallCredentials(token()::getAccessToken);
    }

    private boolean isExpired(Token token) {
        Instant now = clock.instant();
        Instant expiry = token.getExpiry();
        return now.minusSeconds(120).isAfter(expiry);
    }
}
