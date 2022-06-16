package com.wgtwo.auth;

import com.github.scribejava.apis.openid.OpenIdJsonTokenExtractor;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.model.OAuth2AccessToken;

class WgtwoApi extends DefaultApi20 {
    private final String host;

    public WgtwoApi(String host) {
        this.host = host;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return host + "/oauth2/token";
    }

    @Override
    public String getAuthorizationBaseUrl() {
        return host + "/oauth2/auth";
    }

    @Override
    public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() {
        return OpenIdJsonTokenExtractor.instance();
    }
}
