package com.wgtwo.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.wgtwo.auth.model.AccessTokenException;
import com.wgtwo.auth.model.Token;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Delay;

class ClientCredentialsTest {
  private final Clock clock = Clock.fixed(Instant.parse("2022-06-16T10:07:26Z"), ZoneOffset.UTC);

  private ClientAndServer mockServer;

  @BeforeEach
  public void setup() {
    mockServer = startClientAndServer();
  }

  @AfterEach
  public void after() {
    mockServer.stop();
  }

  @Test
  public void shouldReturnAccessTokenOnValidRequest() {
    mockServer
        .when(
            request()
                .withMethod("POST")
                .withPath("/oauth2/token")
                .withHeader(
                    "Authorization",
                    "Basic"
                        + " ODZiNzZjYjUtY2FmMy00YzFjLWJkZmUtOGIzNDU0ZjU4MGI4OlJ6dmNiUURmcmtPYjVFbHZQdXhBNDlvQThvZEJaZnZqNU1LMXIyQWRGdVY5OUVHdkw0YUp2QVJVZzYzN3AzUXFxZ3JVNmd5Rw==")
                .withBody(
                    "scope=subscription.handset_details%3Aread&grant_type=client_credentials"))
        .respond(
            response()
                .withStatusCode(200)
                .withBody(
                    "{\"access_token\":\"bwZyW0-M6RXOqInIZej2whGk0DEtuM8XvP35bPbE-xw.D6FkDLByvChH3UNdTSmzok7Q8ZuxpDVnpQgpddc_GEU\",\"expires_in\":3599,\"scope\":\"subscription.handset_details:read\",\"token_type\":\"bearer\"}")
                .withDelay(Delay.milliseconds(250)));

    WgtwoAuth wgtwoAuth =
        WgtwoAuth.builder(
                "86b76cb5-caf3-4c1c-bdfe-8b3454f580b8",
                "RzvcbQDfrkOb5ElvPuxA49oA8odBZfvj5MK1r2AdFuV99EGvL4aJvARUg637p3QqqgrU6gyG")
            .clock(clock)
            .oauthServer("http://127.0.0.1:" + mockServer.getLocalPort())
            .build();
    Token token = wgtwoAuth.clientCredentials.fetchToken("subscription.handset_details:read");
    assertThat(token.getAccessToken())
        .isEqualTo(
            "bwZyW0-M6RXOqInIZej2whGk0DEtuM8XvP35bPbE-xw.D6FkDLByvChH3UNdTSmzok7Q8ZuxpDVnpQgpddc_GEU");
    assertThat(token.getExpiry()).isEqualTo(clock.instant().plusSeconds(3599));
    assertThat(token.getScope()).isEqualTo("subscription.handset_details:read");
  }

  @Test
  public void invalidRequest() {
    mockServer
        .when(request().withMethod("POST").withPath("/oauth2/token"))
        .respond(
            response()
                .withStatusCode(401)
                .withBody(
                    "{\"error\":\"invalid_client\",\"error_description\":\"Client authentication"
                        + " failed (e.g., unknown client, no client authentication included, or"
                        + " unsupported authentication method).\"}")
                .withDelay(Delay.milliseconds(250)));

    WgtwoAuth wgtwoAuth =
        WgtwoAuth.builder("invalid", "invalid")
            .clock(clock)
            .oauthServer("http://127.0.0.1:" + mockServer.getLocalPort())
            .build();

    assertThatThrownBy(() -> wgtwoAuth.clientCredentials.fetchToken(null))
        .isInstanceOf(AccessTokenException.class)
        .matches(e -> Objects.equals(((AccessTokenException) e).getError(), "invalid_client"));
  }
}
