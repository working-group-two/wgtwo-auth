package com.wgtwo.auth;

import com.wgtwo.api.v1.events.EventsProto;
import com.wgtwo.api.v1.subscription.SubscriptionEventServiceGrpc;
import com.wgtwo.api.v1.subscription.SubscriptionEventsProto;
import com.wgtwo.api.v1.subscription.SubscriptionEventsProto.StreamHandsetChangeEventsResponse;
import com.wgtwo.auth.model.Token;
import io.grpc.CallCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.Delay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

class ClientCredentialsSourceTest {
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
    public void shouldCacheUsingClientCredentialsSource() {
        mockServer.when(
                request()
                        .withMethod("POST")
                        .withPath("/oauth2/token")
                        .withHeader("Authorization", "Basic ODZiNzZjYjUtY2FmMy00YzFjLWJkZmUtOGIzNDU0ZjU4MGI4OlJ6dmNiUURmcmtPYjVFbHZQdXhBNDlvQThvZEJaZnZqNU1LMXIyQWRGdVY5OUVHdkw0YUp2QVJVZzYzN3AzUXFxZ3JVNmd5Rw==")
                        .withBody("scope=subscription.handset_details%3Aread&grant_type=client_credentials"),
                Times.exactly(1)
        ).respond(
                response()
                        .withStatusCode(200)
                        .withBody("{\"access_token\":\"my-access-token\",\"expires_in\":3599,\"scope\":\"subscription.handset_details:read\",\"token_type\":\"bearer\"}")
                        .withDelay(Delay.milliseconds(250))
        );

        WgtwoAuth wgtwoAuth = WgtwoAuth.builder("86b76cb5-caf3-4c1c-bdfe-8b3454f580b8", "RzvcbQDfrkOb5ElvPuxA49oA8odBZfvj5MK1r2AdFuV99EGvL4aJvARUg637p3QqqgrU6gyG")
                .oauthServer("http://127.0.0.1:" + mockServer.getLocalPort())
                .build();

        ClientCredentialSource clientCredentialSource = wgtwoAuth.clientCredentials.newTokenSource("subscription.handset_details:read");

        for (int i = 0; i < 1000; i++) {
            Token token = clientCredentialSource.fetchToken();
            assertThat(token.getAccessToken()).isEqualTo("my-access-token");
        }
    }

    @Disabled
    @Test
    public void shouldInjectToGrpcStub() throws Exception {
        String clientId = envOrEmpty("CLIENT_ID");
        String clientSecret = envOrEmpty("CLIENT_SECRET");
        WgtwoAuth wgtwoAuth = WgtwoAuth.builder(clientId, clientSecret).build();

        ClientCredentialSource clientCredentialSource = wgtwoAuth.clientCredentials.newTokenSource("subscription.handset_details:read");
        CallCredentials callCredentials = clientCredentialSource.callCredentials();

        ManagedChannel channel = ManagedChannelBuilder.forTarget("sandbox.api.wgtwo.com:443").build();

        SubscriptionEventServiceGrpc.SubscriptionEventServiceStub stub
                = SubscriptionEventServiceGrpc.newStub(channel).withCallCredentials(callCredentials);

        SubscriptionEventsProto.StreamHandsetChangeEventsRequest request = SubscriptionEventsProto.StreamHandsetChangeEventsRequest.newBuilder()
                .setStreamConfiguration(EventsProto.StreamConfiguration.getDefaultInstance())
                .build();

        stub.streamHandsetChangeEvents(request, new StreamObserver<StreamHandsetChangeEventsResponse>() {
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
