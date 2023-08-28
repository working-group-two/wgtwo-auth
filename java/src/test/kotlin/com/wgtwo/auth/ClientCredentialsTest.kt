package com.wgtwo.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.Delay
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse

class ClientCredentialsTest {

    private lateinit var mockServer: ClientAndServer

    @BeforeEach
    fun setup() {
        mockServer = ClientAndServer.startClientAndServer()
    }

    @AfterEach
    fun after() {
        mockServer.stop()
    }

    @Test
    fun `should obtain access token via client credentials flow`() {
        setupMockServer()

        val wgtwoAuth = WgtwoAuth.builder(CLIENT_ID, CLIENT_SECRET)
            .oauthServer("http://127.0.0.1:" + mockServer.localPort)
            .build()

        val clientCredentialSource = wgtwoAuth.clientCredentials.newTokenSource("subscription.handset_details:read")

        val token = clientCredentialSource.fetchToken()
        assertThat(token.accessToken)
            .isEqualTo("ih_iwZar30-sjSJiJkBRsNePNZ_MGjhmhgAwMg6tLr0.YzzYG3UIUOb9W8XFZkUQ1S0OuIJE5mvmSGsO1cBx_RE")
    }

    private fun setupMockServer() {
        mockServer.`when`(
            HttpRequest.request()
                .withMethod("POST")
                .withPath("/oauth2/token")
                .withHeader(
                    "Authorization",
                    "Basic ${base64("$CLIENT_ID:$CLIENT_SECRET")}",
                )
                .withBody("scope=subscription.handset_details%3Aread&grant_type=client_credentials"),
            Times.exactly(1),
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withBody(javaClass.getResource("/access-token-with-id-token.json")!!.readText())
                .withDelay(Delay.milliseconds(250)),
        )
    }

    companion object {
        const val CLIENT_ID = "86b76cb5-caf3-4c1c-bdfe-8b3454f580b8"
        const val CLIENT_SECRET = "RzvcbQDfrkOb5ElvPuxA49oA8odBZfvj5MK1r2AdFuV99EGvL4aJvARUg637p3QqqgrU6gyG"

        private fun base64(value: String): String = java.util.Base64.getEncoder().encodeToString(value.toByteArray())
    }
}
