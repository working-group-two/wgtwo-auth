# Java library for authentication towards https://api.wgtwo.com

## Client Credentials flow
```kotlin
val clientId = "86b76cb5-caf3-4c1c-bdfe-8b3454f580b8"
val clientSecret = "RzvcbQDfrkOb5ElvPuxA49oA8odBZfvj5MK1r2AdFuV99EGvL4aJvARUg637p3QqqgrU6gyG"
val scope = "subscription.country_change:read subscription.handset_details:read"


val wgtwoAuth = WgtwoAuth.builder(clientId, clientSecret).build()
val token = wgtwoAuth.clientCredentials.accessToken(scope)
```

## Client Credentials flow with cache

```kotlin
val clientId = "86b76cb5-caf3-4c1c-bdfe-8b3454f580b8"
val clientSecret = "RzvcbQDfrkOb5ElvPuxA49oA8odBZfvj5MK1r2AdFuV99EGvL4aJvARUg637p3QqqgrU6gyG"
val wgtwoAuth = WgtwoAuth.builder(clientId, clientSecret).build()

// Keep alive needed for reliable event streaming
val channel = ManagedChannelBuilder.forTarget("api.wgtwo.com:443")
    .keepAliveWithoutCalls(true)
    .keepAliveTime(1, TimeUnit.MINUTES)
    .keepAliveTimeout(10, TimeUnit.SECONDS)
    .idleTimeout(1, TimeUnit.HOURS)
    .build()

val clientCredentialSource = wgtwoAuth.clientCredentialSource("subscription.handset_details:read")
val callCredentials = clientCredentialSource.callCredentials()
val stub = SubscriptionEventServiceGrpc.newStub(channel).withCallCredentials(callCredentials)

val request = streamHandsetChangeEventsRequest { }
stub.streamHandsetChangeEvents(request, observer)
```

## Authorization Code flow
When using the authorization code flow, `callbackUri` must be set.

If the scope `openid` is set, a openId token will be returned.
The `nonce` claim will include the nonce you set in the parameters.

### Create authorization url
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

### Redirect user to authorization url
Redirect user to ${authorizationUrl} in browser
After login the user is redirected back to https://example.com/oauth/callback with query parameters:

On errors these will be:
- `state` - Should match state passed above
- `error` - Error enun
- `error_description` - Helpful error description

On success these will be:
- `state` - Should match state passed above
- `code` - Authorization code
- `scope` - The granted scopes

Note that according to the OAuth 2.0 spec, the granted scope may differ from the requested scopes.

### Exchange code for token
```
var parameters = splitQueryParams("https://example.com/oauth/callback?state=my-state&code=(...)&scope=(...)

var state = parameters["state"]
if (!Objects.equals(state, expectedState) {
  throw InvalidStateException()
}

var code = parameters["code"]
var token = wgtwoAuth.clientCredentials.accessToken(code)
```
