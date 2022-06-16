package com.wgtwo.auth.model;

import com.wgtwo.auth.Prompt;
import com.wgtwo.auth.WgtwoAuth;
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
    private final Clock clock = Clock.fixed(Instant.parse("2022-06-16T10:07:26Z"), ZoneOffset.UTC);

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

        assertThat(authorizationUrl)
                .isEqualTo("http://127.0.0.1:" + mockServer.getLocalPort()+ "/oauth2/auth?nonce=my-nonce&response_type=code&client_id=clientId&redirect_uri=https%3A%2F%2Fexample.com%2Foauth%2Fcallback&scope=phone%20openid&state=my-state");

    }

    @Test
    public void shouldExchangeAuthorizationCodeForAccessTokenWithoutOpenIdToken() {
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
                        .withBody("{\"access_token\":\"ih_iwZar30-sjSJiJkBRsNePNZ_MGjhmhgAwMg6tLr0.YzzYG3UIUOb9W8XFZkUQ1S0OuIJE5mvmSGsO1cBx_RE\",\"expires_in\":3599,\"scope\":\"phone\",\"token_type\":\"bearer\"}")
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
                        .withBody("{\"access_token\":\"ih_iwZar30-sjSJiJkBRsNePNZ_MGjhmhgAwMg6tLr0.YzzYG3UIUOb9W8XFZkUQ1S0OuIJE5mvmSGsO1cBx_RE\",\"expires_in\":3599,\"id_token\":\"eyJhbGciOiJSUzI1NiIsImtpZCI6InB1YmxpYzo0ZjYwZWNhNy1hNWRlLTQ2MTctYTQ1Ni05YzEyMGI5MWQ4MzkiLCJ0eXAiOiJKV1QifQ.eyJhdF9oYXNoIjoiUTlwekxVb3ZOWWFVX1Y1S24tN1lTZyIsImF1ZCI6WyJiN2U2N2M0Ny04ZGZjLTQxODAtOGZlZi0zMzI2YTJjNzk5ODUiXSwiYXV0aF90aW1lIjoxNjU1Mzg3NzgzLCJleHAiOjE2NTUzOTIzMzIsImlhdCI6MTY1NTM4ODczMiwiaXNzIjoiaHR0cHM6Ly9pZC53Z3R3by5jb20vIiwianRpIjoiNWY1NWQ1OWItMGZiYy00ZWU2LTljYmMtNmUxNDU0YTAwY2RjIiwibm9uY2UiOiJteS1ub25jZSIsInBob25lX251bWJlciI6Iis0Nzk5OTk5OTk5IiwicGhvbmVfbnVtYmVyX3ZlcmlmaWVkIjp0cnVlLCJyYXQiOjE2NTUzODg2NzQsInNpZCI6IjIzNWU2MTVjLWM1MmQtNDA3Yy04NzYwLTdmZjEyNTZkMGU2NiIsInN1YiI6IjRjNDJiNWViYjgxZjZmMjc1MDk4ZDdkMTlhY2I5MmRiZTNlNGY3MGVkYjk3MWI2MWY5ZmY0MmU2NGZmZWQxODAxZGZjNzJmZWI0NDI1NDczNjBmN2QwYWY4MmYwYzkxMjUzZTdhMzUwMDNkNzQxZjRlNjEwMGQ5ZWQ1Y2IzZDc4In0.r8OrIVWRyXalSdVM22TuNL5FWOCEumIO6Hk_N33qmKrMe0nyw4-wWMBlYck2undjILiRU7zXRi-5lJY-mCX09qeKISFbxcnRkiM0CmFoElHB28vpeTwSgL7V7IEPi-nj2AADv-Zqvf0Cn6lpBjEAHv8K8LM8HB944yR9c96yfIWuJFRr5RVeLMY3OBaW683riw3GrLwyHC2P6toQAOynjsxomDYL-dg-e28LQvyyiKyF8rZRGoZf9AnPmMGgi32-1UAtzpkBDhpmxCtnIXc_uuuXd5UUw2qJQYa6SN-FS8c9BA5EmMv9ci3qR5ijqKOVaM_4hwIzhacgksBLQ4A-pAv3X-qsDJ_N9nyAdeuK34mSRNY9e8X4u8EfzMulvcffoyXRjbCtGyZcb5rmQifoCz5flJmuV_nSYbtUg1VIXt7V0pp_ZzHGvxNqwZTfypa5itGf212-Kd2BasH7xB6Oeyjj5igsgdlyqezuZCJuW_-NzUUAb7LEcYO8UXnEb-arHBy-wBRYCWyShUZb5yIK8xcZ-jD28jnnEpEdkW12xdiB8Vw-kivr1E1ey2OdETWtINRLEt7m5YrI_qnnftH4pqWayt92ppIKR8wD4NOd0QMP3pIm8b0cxscBssefV6EW6DC8VJxsAwdYI28efQ2QCl-JWzDEFQdYiJ1uNZnJQKQ\",\"scope\":\"phone openid\",\"token_type\":\"bearer\"}")
                        .withDelay(Delay.milliseconds(250))
        );

        WgtwoAuth wgtwoAuth = WgtwoAuth.builder("my-client-id", "my-client-secret")
                .callbackUri("https://localhost:30000/oauth/callback")
                .oauthServer("http://127.0.0.1:" + mockServer.getLocalPort())
                .build();
        Token token = wgtwoAuth.authorizationCode.accessToken("7CjFbYFYpr9IJ0CUJTlKZvt3kGr1k2fY47zWm8_Jn2o.hw8mJQKNVXU2jRprG-2bv4ppw8DVXzo4COQbXgKTbN0");

        assertThat(token.getAccessToken()).isEqualTo("ih_iwZar30-sjSJiJkBRsNePNZ_MGjhmhgAwMg6tLr0.YzzYG3UIUOb9W8XFZkUQ1S0OuIJE5mvmSGsO1cBx_RE");
        assertThat(token.getScope()).isEqualTo("phone openid");
        assertThat(token.getScope()).isEqualTo("phone openid");

        assertThat(token.getMetadata()).isNotNull();
        assertThat(token.getMetadata().getPhone()).isEqualTo("+4799999999");
        assertThat(token.getMetadata().getNonce()).isEqualTo("my-nonce");
    }
}
