package com.wgtwo.auth;

import com.github.scribejava.apis.openid.OpenIdJsonTokenExtractor;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.model.OAuth2AccessToken;
import org.jetbrains.annotations.NotNull;

class WgtwoApi extends DefaultApi20 {
    private final String host;

    public WgtwoApi(@NotNull String host) {
        this.host = host;
    }

    @Override
    @NotNull
    public String getAccessTokenEndpoint() {
        return host + "/oauth2/token";
    }

    @Override
    @NotNull
    public String getAuthorizationBaseUrl() {
        return host + "/oauth2/auth";
    }

    @Override
    @NotNull
    public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() {
        return OpenIdJsonTokenExtractor.instance();
    }
}
