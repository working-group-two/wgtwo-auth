# Java library for authentication towards https://api.wgtwo.com

## Install

### Add the JitPack repository to your build file
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

### Add the dependency
```xml
<dependency>
    <groupId>com.github.working-group-two</groupId>
    <artifactId>wgtwo-auth</artifactId>
    <version>f216e4e463</version>
</dependency>
```

## Example usage
### Java

#### Client Credentials flow with gRPC

This will inject a up-to-date access token as call credentials to the gRPC stub.

```java
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
    private static final ClientCredentialSource clientCredentialSource = wgtwoAuth.clientCredentialSource("sms.text:send_to_subscriber");

    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forTarget("api.wgtwo.com:443").build();
        SmsServiceBlockingStub stub = SmsServiceGrpc.newBlockingStub(channel)
                .withCallCredentials(clientCredentialSource.callCredentials());

        SendTextToSubscriberRequest request = SendTextToSubscriberRequest.newBuilder()
                .setContent("Test")
                .setFromAddress("My Product")
                .setToSubscriber("4799990000")
                .build();
        SendMessageResponse response = stub.sendTextToSubscriber(request);
        System.out.println("Send status: " + response.getStatus());

        channel.shutdownNow();
    }
}
```

### Kotlin

#### Client Credentials flow with gRPC

This will inject a up-to-date access token as call credentials to the gRPC stub.

```kotlin
package auth.sample

import com.wgtwo.api.v1.sms.SmsProto.SendTextToSubscriberRequest
import com.wgtwo.api.v1.sms.SmsServiceGrpc
import com.wgtwo.auth.WgtwoAuth
import io.grpc.ManagedChannelBuilder

private val clientId = System.getenv("CLIENT_ID")
private val clientSecret = System.getenv("CLIENT_SECRET")
private val wgtwoAuth = WgtwoAuth.builder(clientId, clientSecret).build()
private val clientCredentialSource = wgtwoAuth.clientCredentialSource("sms.text:send_to_subscriber")
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

```

#### Authorization Code flow
When using the authorization code flow, `callbackUri` must be set.

If the scope `openid` is set, a openId token will be returned.
The `nonce` claim will include the nonce you set in the parameters.

##### Create authorization url
```kotlin
val clientId = "86b76cb5-caf3-4c1c-bdfe-8b3454f580b8"
val clientSecret = "RzvcbQDfrkOb5ElvPuxA49oA8odBZfvj5MK1r2AdFuV99EGvL4aJvARUg637p3QqqgrU6gyG"
val scope = "sms.text:send_from_subscriber"

val wgtwoAuth = WgtwoAuth.builder(clientId, clientSecret)
    .callbackUri("https://example.com/oauth/callback")
    .build()

var scope = "phone openid"
var nonce = "my-nonce"
var state = "my-state"
var authorizationUrl = wgtwoAuth.authorizationCode.authorizationUrl(scope, nonce, state, Prompt.DEFAULT)
```

##### Redirect user to authorization url
Redirect user to ${authorizationUrl} in browser
After login the user is redirected back to https://example.com/oauth/callback with query parameters:

On errors these will be:
- `state` - Should match state passed above
- `error` - Error enum
- `error_description` - Helpful error description

On success these will be:
- `state` - Should match state passed above
- `code` - Authorization code
- `scope` - The granted scopes

Note that according to the OAuth 2.0 spec, the granted scope may differ from the requested scopes.

##### Exchange code for token

```
var parameters = splitQueryParams("https://example.com/oauth/callback?state=my-state&code=(...)&scope=(...)
var state = parameters["state"]
var code = parameters["code"]

if (!Objects.equals(state, expectedState) {
  throw InvalidStateException()
}

var token = wgtwoAuth.clientCredentials.accessToken(code)
```
