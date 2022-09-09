package auth.usage

import com.wgtwo.api.v1.sms.SmsServiceGrpc
import io.grpc.ManagedChannelBuilder

val channel = ManagedChannelBuilder.forTarget("localhost:8080").build()

// Get token
private val token = wgtwoAuth.clientCredentials.fetchToken("phone sms.text:send_to_subscriber")

// Get token source (cache with automatic refresh)
val tokenSource = wgtwoAuth.clientCredentials.newTokenSource("phone sms.text:send_to_subscriber")
val cachedToken = tokenSource.fetchToken()

// Create call credentials for gRPC
val callCredentials = tokenSource.callCredentials()

// Include call credentials in gRPC stub
val stubWithCallCredentials = SmsServiceGrpc.newBlockingStub(channel).withCallCredentials(callCredentials)
