package com.wgtwo.auth;

import com.google.common.io.Resources;
import com.wgtwo.auth.Prompt;
import com.wgtwo.auth.WgtwoAuth;
import com.wgtwo.auth.model.Token;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Delay;
import org.mockserver.model.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

class AuthorizationCodeTest {
    private ClientAndServer mockServer;

    @BeforeEach
    public void setup() {
        mockServer = startClientAndServer();
    }

    @AfterEach
    public void after() {
        mockServer.stop();
        mockServer.close();
    }

    @Test
    public void shouldCreateAuthorizationCode() {
        WgtwoAuth wgtwoAuth = WgtwoAuth.builder("clientId", "clientSecret")
                .callbackUri("https://example.com/oauth/callback")
                .oauthServer("http://127.0.0.1:" + mockServer.getLocalPort())
                .build();

        String scope = "phone openid";
        String nonce = "my-nonce";
        String state = "my-state";
        String authorizationUrl = wgtwoAuth.authorizationCode.authorizationUrl(scope, nonce, state, Prompt.DEFAULT);

        assertThat(authorizationUrl).isEqualTo(
                "http://127.0.0.1:" + mockServer.getLocalPort() + "/oauth2/auth" +
                        "?nonce=my-nonce&response_type=code" +
                        "&client_id=clientId" +
                        "&redirect_uri=https%3A%2F%2Fexample.com%2Foauth%2Fcallback" +
                        "&scope=phone%20openid" +
                        "&state=my-state"
        );

    }

    @Test
    public void shouldExchangeAuthorizationCodeForAccessTokenWithoutOpenIdToken() {
        mockServer.when(
                request()
                        .withMethod("POST")
                        .withPath("/oauth2/token")
                        .withHeader("Authorization", "Basic bXktY2xpZW50LWlkOm15LWNsaWVudC1zZWNyZXQ=")
                        .withContentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .withBody("" +
                                "code=7CjFbYFYpr9IJ0CUJTlKZvt3kGr1k2fY47zWm8_Jn2o.hw8mJQKNVXU2jRprG-2bv4ppw8DVXzo4COQbXgKTbN0" +
                                "&redirect_uri=https%3A%2F%2Flocalhost%3A30000%2Foauth%2Fcallback" +
                                "&grant_type=authorization_code"
                        )
        ).respond(
                response()
                        .withStatusCode(200)
                        .withBody(resource("access-token.json"))
                        .withDelay(Delay.milliseconds(250))
        );

        WgtwoAuth wgtwoAuth = WgtwoAuth.builder("my-client-id", "my-client-secret")
                .callbackUri("https://localhost:30000/oauth/callback")
                .oauthServer("http://127.0.0.1:" + mockServer.getLocalPort())
                .build();

        Token token = wgtwoAuth.authorizationCode.accessToken("7CjFbYFYpr9IJ0CUJTlKZvt3kGr1k2fY47zWm8_Jn2o.hw8mJQKNVXU2jRprG-2bv4ppw8DVXzo4COQbXgKTbN0");
        assertThat(token.getMetadata()).isNull();
        assertThat(token.getAccessToken()).isEqualTo("ih_iwZar30-sjSJiJkBRsNePNZ_MGjhmhgAwMg6tLr0.YzzYG3UIUOb9W8XFZkUQ1S0OuIJE5mvmSGsO1cBx_RE");
        assertThat(token.getScope()).isEqualTo("phone");
    }

    @Test
    public void shouldExchangeAuthorizationCodeForAccessTokenIncludingOpenIdToken() {
        mockServer.when(
                request()
                        .withMethod("POST")
                        .withPath("/oauth2/token")
                        .withHeader("Authorization", "Basic bXktY2xpZW50LWlkOm15LWNsaWVudC1zZWNyZXQ=")
                        .withContentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .withBody("code=7CjFbYFYpr9IJ0CUJTlKZvt3kGr1k2fY47zWm8_Jn2o.hw8mJQKNVXU2jRprG-2bv4ppw8DVXzo4COQbXgKTbN0&redirect_uri=https%3A%2F%2Flocalhost%3A30000%2Foauth%2Fcallback&grant_type=authorization_code")
        ).respond(
                response()
                        .withStatusCode(200)
                        .withBody(resource("access-token-with-id-token.json"))
                        .withDelay(Delay.milliseconds(250))
        );

        WgtwoAuth wgtwoAuth = WgtwoAuth.builder("my-client-id", "my-client-secret")
                .callbackUri("https://localhost:30000/oauth/callback")
                .oauthServer("http://127.0.0.1:" + mockServer.getLocalPort())
                .build();
        Token token = wgtwoAuth.authorizationCode.accessToken("7CjFbYFYpr9IJ0CUJTlKZvt3kGr1k2fY47zWm8_Jn2o.hw8mJQKNVXU2jRprG-2bv4ppw8DVXzo4COQbXgKTbN0");

        assertThat(token.getAccessToken()).isEqualTo("ih_iwZar30-sjSJiJkBRsNePNZ_MGjhmhgAwMg6tLr0.YzzYG3UIUOb9W8XFZkUQ1S0OuIJE5mvmSGsO1cBx_RE");
        assertThat(token.getScope()).isEqualTo("phone openid");

        assertThat(token.getMetadata()).isNotNull();
        assertThat(token.getMetadata().getPhone()).isEqualTo("+4799999999");
        assertThat(token.getMetadata().getNonce()).isEqualTo("my-nonce");
    }

    private String resource(String resource) {
        URL url = Resources.getResource(resource);
        try {
            return Resources.toString(url, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
