package auth

import com.wgtwo.auth.WgtwoAuth
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

        val clientId = "86b76cb5-caf3-4c1c-bdfe-8b3454f580b8"
        val clientSecret = "RzvcbQDfrkOb5ElvPuxA49oA8odBZfvj5MK1r2AdFuV99EGvL4aJvARUg637p3QqqgrU6gyG"
        val wgtwoAuth = WgtwoAuth.builder(clientId,clientSecret)
            .oauthServer("http://127.0.0.1:" + mockServer.localPort)
            .build()

        val clientCredentialSource = wgtwoAuth.clientCredentials.tokenSource("subscription.handset_details:read")

        val token = clientCredentialSource.token()
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
                    "Basic ODZiNzZjYjUtY2FmMy00YzFjLWJkZmUtOGIzNDU0ZjU4MGI4OlJ6dmNiUURmcmtPYjVFbHZQdXhBNDlvQThvZEJaZnZqNU1LMXIyQWRGdVY5OUVHdkw0YUp2QVJVZzYzN3AzUXFxZ3JVNmd5Rw=="
                )
                .withBody("scope=subscription.handset_details%3Aread&grant_type=client_credentials"),
            Times.exactly(1)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withBody(javaClass.getResource("/access-token-with-id-token.json")!!.readText())
                .withDelay(Delay.milliseconds(250))
        )
    }
}
