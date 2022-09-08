package com.wgtwo.auth;

import com.wgtwo.api.v1.sms.SmsProto;
import com.wgtwo.api.v1.sms.SmsProto.SendMessageResponse;
import com.wgtwo.api.v1.sms.SmsProto.SendTextToSubscriberRequest;
import com.wgtwo.api.v1.sms.SmsServiceGrpc;
import com.wgtwo.auth.BearerTokenCallCredentials;
import com.wgtwo.auth.ClientCredentialSource;
import com.wgtwo.auth.WgtwoAuth;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class Sample {
    private final String clientId = System.getenv("CLIENT_ID");
    private final String clientSecret = System.getenv("CLIENT_SECRET");
    private final WgtwoAuth wgtwoAuth = WgtwoAuth.builder(clientId, clientSecret).build();
    private final ClientCredentialSource clientCredentialSource = wgtwoAuth.clientCredentialSource("sms.text:send_to_subscriber");
    private final BearerTokenCallCredentials callCredentials = clientCredentialSource.callCredentials();
    private final ManagedChannel channel = ManagedChannelBuilder.forTarget("api.wgtwo.com:443").build();
    private final SmsServiceGrpc.SmsServiceBlockingStub stub = SmsServiceGrpc.newBlockingStub(channel)
            .withCallCredentials(callCredentials);

    public void sendSms() {
        SendTextToSubscriberRequest request = SendTextToSubscriberRequest.newBuilder()
                .setContent("Test")
                .setFromAddress("My Product")
                .setToSubscriber("4799990000")
                .build();
        SendMessageResponse response = stub.sendTextToSubscriber(request);
    }
}
