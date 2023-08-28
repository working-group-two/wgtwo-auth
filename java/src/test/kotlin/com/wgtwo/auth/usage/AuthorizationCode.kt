package com.wgtwo.auth.usage

import com.wgtwo.auth.Prompt
import com.wgtwo.auth.model.Token

const val SCOPE = "offline_access phone sms.text:send_to_subscriber"
const val NONCE = "random-nonce"
const val STATE = "random-state"

// Shows login and consent depending on session and consent available
val prompt = Prompt.DEFAULT

// Redirect user to the authorization URL
val authorizationUrl: String = wgtwoAuth.authorizationCode.createAuthorizationUrl(SCOPE, NONCE, STATE, prompt)

// The user will be redirected back to you. If successful, this will have `code` as a query param
val userToken: Token = wgtwoAuth.authorizationCode.fetchToken("code from query param")

val accessToken: String = userToken.accessToken
val refreshToken: String? = userToken.refreshToken

val newToken: Token = wgtwoAuth.authorizationCode.refreshToken(refreshToken!!)
