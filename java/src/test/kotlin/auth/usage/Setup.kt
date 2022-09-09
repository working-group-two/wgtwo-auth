package auth.usage

import com.wgtwo.auth.WgtwoAuth

val clientId = System.getenv("CLIENT_ID")
val clientSecret = System.getenv("CLIENT_SECRET")
val wgtwoAuth = WgtwoAuth.builder(clientId, clientSecret).build()
