package com.wgtwo.auth.sample;

import com.wgtwo.api.v1.sms.SmsProto.SendMessageResponse;
import com.wgtwo.api.v1.sms.SmsProto.SendTextToSubscriberRequest;
import com.wgtwo.api.v1.sms.SmsServiceGrpc;
import com.wgtwo.api.v1.sms.SmsServiceGrpc.SmsServiceBlockingStub;
import com.wgtwo.auth.ClientCredentialSource;
import com.wgtwo.auth.WgtwoAuth;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class Sample {
  private static final String clientId = System.getenv("CLIENT_ID");
  private static final String clientSecret = System.getenv("CLIENT_SECRET");
  private static final WgtwoAuth wgtwoAuth = WgtwoAuth.builder(clientId, clientSecret).build();
  private static final ClientCredentialSource clientCredentialSource =
      wgtwoAuth.clientCredentials.newTokenSource("sms.text:send_to_subscriber");

  public static void main(String[] args) {
    ManagedChannel channel = ManagedChannelBuilder.forTarget("api.wgtwo.com:443").build();
    SmsServiceBlockingStub stub =
        SmsServiceGrpc.newBlockingStub(channel)
            .withCallCredentials(clientCredentialSource.callCredentials());

    SendTextToSubscriberRequest request =
        SendTextToSubscriberRequest.newBuilder()
            .setContent("Test")
            .setFromAddress("My Product")
            .setToSubscriber("4799990000")
            .build();
    SendMessageResponse response = stub.sendTextToSubscriber(request);
    System.out.println("Send status: " + response.getStatus());

    channel.shutdownNow();
  }
}
