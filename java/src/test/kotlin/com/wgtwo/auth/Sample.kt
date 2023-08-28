package com.wgtwo.auth

import com.wgtwo.api.v1.sms.SmsProto.SendTextToSubscriberRequest
import com.wgtwo.api.v1.sms.SmsServiceGrpc
import io.grpc.ManagedChannelBuilder

private val clientId = System.getenv("CLIENT_ID")
private val clientSecret = System.getenv("CLIENT_SECRET")
private val wgtwoAuth = WgtwoAuth.builder(clientId, clientSecret).build()
private val clientCredentialSource = wgtwoAuth.clientCredentials.newTokenSource("sms.text:send_to_subscriber")
private val callCredentials = clientCredentialSource.callCredentials()

fun main() {
    val channel = ManagedChannelBuilder.forTarget("api.wgtwo.com:443").build()
    val stub = SmsServiceGrpc.newBlockingStub(channel)
        .withCallCredentials(callCredentials)

    val request = SendTextToSubscriberRequest.newBuilder().apply {
        content = "Test"
        fromAddress = "My Product"
        toSubscriber = "4799990000"
    }.build()
    val response = stub.sendTextToSubscriber(request)
    println("Sent status: ${response.status}")

    channel.shutdownNow()
}
