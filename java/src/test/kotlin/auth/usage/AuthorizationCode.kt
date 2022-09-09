package auth.usage

import com.wgtwo.auth.Prompt
import com.wgtwo.auth.model.Token

val scope = "offline_access phone sms.text:send_to_subscriber"
val nonce = "random-nonce"
val state = "random-state"

// Shows login and consent depending on session and consent available
val prompt = Prompt.DEFAULT

// Redirect user to the authorization URL
val authorizationUrl: String = wgtwoAuth.authorizationCode.createAuthorizationUrl(scope, nonce, state, prompt)

// The user will be redirected back to you. If successful, this will have `code` as a query param
private val token: Token = wgtwoAuth.authorizationCode.fetchToken("code from query param")

val accessToken: String = token.accessToken
val refreshToken: String? = token.refreshToken

val newToken: Token = wgtwoAuth.authorizationCode.refreshToken(refreshToken!!)
