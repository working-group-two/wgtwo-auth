package auth

import com.wgtwo.api.v1.sms.SmsProto.SendTextToSubscriberRequest
import com.wgtwo.api.v1.sms.SmsServiceGrpc
import com.wgtwo.auth.WgtwoAuth
import io.grpc.ManagedChannelBuilder

class Sample {
    private val clientId = System.getenv("CLIENT_ID")
    private val clientSecret = System.getenv("CLIENT_SECRET")
    private val wgtwoAuth = WgtwoAuth.builder(clientId, clientSecret).build()
    private val clientCredentialSource = wgtwoAuth.clientCredentialSource("sms.text:send_to_subscriber")
    private val callCredentials = clientCredentialSource.callCredentials()

    private val channel = ManagedChannelBuilder.forTarget("api.wgtwo.com:443").build()
    private val stub = SmsServiceGrpc.newBlockingStub(channel)
        .withCallCredentials(callCredentials)

    fun sendSms() {
        val request = SendTextToSubscriberRequest.newBuilder().apply {
            content = "Test"
            fromAddress = "My Product"
            toSubscriber = "4799990000"
        }.build()
        val response = stub.sendTextToSubscriber(request)
    }
}
