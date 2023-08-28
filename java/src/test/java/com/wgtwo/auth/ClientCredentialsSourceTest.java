package com.wgtwo.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.wgtwo.api.v1.events.EventsProto;
import com.wgtwo.api.v1.subscription.SubscriptionEventServiceGrpc;
import com.wgtwo.api.v1.subscription.SubscriptionEventsProto;
import com.wgtwo.api.v1.subscription.SubscriptionEventsProto.StreamHandsetChangeEventsResponse;
import com.wgtwo.auth.model.Token;
import io.grpc.CallCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.Delay;

class ClientCredentialsSourceTest {
  private static final Duration DEFAULT_VALID_TOKEN_DURATION = Duration.ofMinutes(60);
  private static final Duration REFRESH_PERIOD = Duration.ofMinutes(2);
  private static final Duration WITHIN_REFRESH_PERIOD = REFRESH_PERIOD.minusSeconds(1);
  private static final String SCOPE_SUBSCRIPTION_HANDSET_DETAILS_READ =
      "subscription.handset_details:read";
  private static final String MY_ACCESS_TOKEN_1 = "my-access-token1";
  private static final String MY_ACCESS_TOKEN_2 = "my-access-token2";
  private final FakeClock clock = FakeClock.forInstant(Instant.parse("2022-06-16T10:07:26Z"));

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
  public void shouldCacheUsingClientCredentialsSource() {
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
                    "scope=subscription.handset_details%3Aread&grant_type=client_credentials"),
            Times.exactly(1))
        .respond(
            response()
                .withStatusCode(200)
                .withBody(
                    "{\"access_token\":\"my-access-token\",\"expires_in\":3599,\"scope\":\""
                        + SCOPE_SUBSCRIPTION_HANDSET_DETAILS_READ
                        + "\",\"token_type\":\"bearer\"}")
                .withDelay(Delay.milliseconds(250)));

    WgtwoAuth wgtwoAuth =
        WgtwoAuth.builder(
                "86b76cb5-caf3-4c1c-bdfe-8b3454f580b8",
                "RzvcbQDfrkOb5ElvPuxA49oA8odBZfvj5MK1r2AdFuV99EGvL4aJvARUg637p3QqqgrU6gyG")
            .oauthServer("http://127.0.0.1:" + mockServer.getLocalPort())
            .build();

    ClientCredentialSource clientCredentialSource =
        wgtwoAuth.clientCredentials.newTokenSource(SCOPE_SUBSCRIPTION_HANDSET_DETAILS_READ);

    for (int i = 0; i < 1000; i++) {
      Token token = clientCredentialSource.fetchToken();
      assertThat(token.getAccessToken()).isEqualTo("my-access-token");
    }
  }

  @Test
  void shouldRefreshAccessTokenWithinRefreshIntervalAndNotExpired() {
    createTwoTokens();

    WgtwoAuth wgtwoAuth = createWgTwoAuthWithFakeClock();

    ClientCredentialSource clientCredentialSource =
        wgtwoAuth.clientCredentials.newTokenSource(SCOPE_SUBSCRIPTION_HANDSET_DETAILS_READ);
    Token token = clientCredentialSource.fetchToken();
    assertThat(token.getAccessToken()).isEqualTo(MY_ACCESS_TOKEN_1);

    // Act
    clock.tick(DEFAULT_VALID_TOKEN_DURATION.minus(WITHIN_REFRESH_PERIOD));
    assertThat(clientCredentialSource.fetchToken().getAccessToken())
        .describedAs("Creating a new access token")
        .isEqualTo(MY_ACCESS_TOKEN_2);
  }

  @Test
  void shouldRefreshAccessTokenAfterExpired() {
    createTwoTokens();

    WgtwoAuth wgtwoAuth = createWgTwoAuthWithFakeClock();

    ClientCredentialSource clientCredentialSource =
        wgtwoAuth.clientCredentials.newTokenSource(SCOPE_SUBSCRIPTION_HANDSET_DETAILS_READ);
    Token token = clientCredentialSource.fetchToken();
    assertThat(token.getAccessToken()).isEqualTo(MY_ACCESS_TOKEN_1);

    // Act
    clock.tick(DEFAULT_VALID_TOKEN_DURATION.plusSeconds(1));
    token = clientCredentialSource.fetchToken();

    SoftAssertions clockVsExpire = new SoftAssertions();
    clockVsExpire
        .assertThat(token.getExpiry())
        .describedAs("Token is not expired")
        .isAfter(clock.instant());
    clockVsExpire
        .assertThat(clientCredentialSource.fetchToken().getAccessToken())
        .describedAs("Creating a new access token")
        .isEqualTo(MY_ACCESS_TOKEN_2);
    clockVsExpire.assertAll();
  }

  @NotNull
  private WgtwoAuth createWgTwoAuthWithFakeClock() {
    return WgtwoAuth.builder(
            "86b76cb5-caf3-4c1c-bdfe-8b3454f580b8",
            "RzvcbQDfrkOb5ElvPuxA49oA8odBZfvj5MK1r2AdFuV99EGvL4aJvARUg637p3QqqgrU6gyG")
        .oauthServer("http://127.0.0.1:" + mockServer.getLocalPort())
        .clock(clock)
        .build();
  }

  @ParameterizedTest
  @MethodSource("providerOfWithinRefreshIntervalOrLater")
  void shouldCacheAndRefreshWhenWithinRefreshIntervalOrLater(Duration validPeriods) {
    createTwoTokens();

    WgtwoAuth wgtwoAuth = createWgTwoAuthWithFakeClock();

    ClientCredentialSource clientCredentialSource =
        wgtwoAuth.clientCredentials.newTokenSource(SCOPE_SUBSCRIPTION_HANDSET_DETAILS_READ);
    Token token = clientCredentialSource.fetchToken();
    assertThat(token.getAccessToken()).isEqualTo(MY_ACCESS_TOKEN_1);

    // Act: test cache
    clock.tick(DEFAULT_VALID_TOKEN_DURATION.minus(REFRESH_PERIOD));
    assertThat(token.getAccessToken()).isEqualTo(MY_ACCESS_TOKEN_1);

    // Act: verify token is recreated within refresh interval or later
    clock.tick(validPeriods);
    assertThat(clientCredentialSource.fetchToken().getAccessToken()).isEqualTo(MY_ACCESS_TOKEN_2);
  }

  static Stream<Arguments> providerOfWithinRefreshIntervalOrLater() {
    return Stream.of(
        Arguments.of(named("Within refresh period", WITHIN_REFRESH_PERIOD)),
        Arguments.of(named("Exactly at refresh period", REFRESH_PERIOD)),
        Arguments.of(
            named(
                "'within' life time of an access token but after expiry", Duration.ofMinutes(42))),
        Arguments.of(
            named("long after life time of both access token and expiry", Duration.ofDays(1))));
  }

  private void createTwoTokens() {
    createToken(MY_ACCESS_TOKEN_1);
    createToken(MY_ACCESS_TOKEN_2);
  }

  private void createToken(String accessToken) {
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
                    "scope=subscription.handset_details%3Aread&grant_type=client_credentials"),
            Times.exactly(1))
        .respond(
            response()
                .withStatusCode(200)
                .withBody(
                    "{\"access_token\":\""
                        + accessToken
                        + "\",\"expires_in\":3599,\"scope\":\""
                        + SCOPE_SUBSCRIPTION_HANDSET_DETAILS_READ
                        + "\",\"token_type\":\"bearer\"}")
                .withDelay(Delay.milliseconds(250)));
  }

  @Disabled
  @Test
  public void shouldInjectToGrpcStub() throws Exception {
    String clientId = envOrEmpty("CLIENT_ID");
    String clientSecret = envOrEmpty("CLIENT_SECRET");
    WgtwoAuth wgtwoAuth = WgtwoAuth.builder(clientId, clientSecret).build();

    ClientCredentialSource clientCredentialSource =
        wgtwoAuth.clientCredentials.newTokenSource("subscription.handset_details:read");
    CallCredentials callCredentials = clientCredentialSource.callCredentials();

    ManagedChannel channel = ManagedChannelBuilder.forTarget("sandbox.api.wgtwo.com:443").build();

    SubscriptionEventServiceGrpc.SubscriptionEventServiceStub stub =
        SubscriptionEventServiceGrpc.newStub(channel).withCallCredentials(callCredentials);

    SubscriptionEventsProto.StreamHandsetChangeEventsRequest request =
        SubscriptionEventsProto.StreamHandsetChangeEventsRequest.newBuilder()
            .setStreamConfiguration(EventsProto.StreamConfiguration.getDefaultInstance())
            .build();

    stub.streamHandsetChangeEvents(
        request,
        new StreamObserver<StreamHandsetChangeEventsResponse>() {
          @Override
          public void onNext(StreamHandsetChangeEventsResponse response) {
            System.out.println(response);
          }

          @Override
          public void onError(Throwable throwable) {
            System.out.println("Error: " + throwable);
          }

          @Override
          public void onCompleted() {}
        });

    Thread.sleep(30_000);

    channel.shutdownNow();
    wgtwoAuth.close();
  }

  private String envOrEmpty(String key) {
    String value = System.getenv(key);
    return (value == null) ? "" : value;
  }
}
