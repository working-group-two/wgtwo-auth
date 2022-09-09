package com.wgtwo.auth.model;

import java.time.Instant;
import java.util.Objects;
import java.util.StringJoiner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Token {
    private final String accessToken;
    private final String refreshToken;
    private final Instant expiry;
    private final String scope;
    private final Metadata metadata;

    public Token(String accessToken, String refreshToken, Instant expiry, String scope, Metadata metadata) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiry = expiry;
        this.scope = scope;
        this.metadata = metadata;
    }

    public Token(String accessToken, String refreshToken, Instant expiry, String scope) {
        this(accessToken, refreshToken, expiry, scope, null);
    }

    @NotNull
    public String getAccessToken() {
        return accessToken;
    }

    @Nullable
    public String getRefreshToken() {
        return refreshToken;
    }

    @NotNull
    public Instant getExpiry() {
        return expiry;
    }

    @NotNull
    public String getScope() {
        return scope;
    }

    @Nullable
    public Metadata getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return Objects.equals(accessToken, token.accessToken)
                && Objects.equals(refreshToken, token.refreshToken)
                && Objects.equals(expiry, token.expiry)
                && Objects.equals(scope, token.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, refreshToken, expiry, scope, metadata);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Token.class.getSimpleName() + "[", "]")
                .add("accessToken='***'")
                .add("refreshToken='" + ((refreshToken == null) ? null : "***") + "'")
                .add("expiry=" + expiry)
                .add("scope='" + scope + "'")
                .add("metadata='" + metadata + "'")
                .toString();
    }
}
