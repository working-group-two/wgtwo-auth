package com.wgtwo.auth

import com.google.common.util.concurrent.MoreExecutors
import com.wgtwo.testing.chrono.FakeClock
import com.wgtwo.testing.chrono.minutes
import io.grpc.CallCredentials
import io.grpc.Metadata
import io.grpc.Status
import io.mockk.mockk
import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.Delay
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse

class BearerTokenCallCredentialsTest {

    @BeforeEach
    fun setup() {
        mockServer.reset()
        mockServerReply("first", "second", "third")
    }

    @Test
    fun `refresh token updates`() {
        val clock = FakeClock(Instant.parse("2000-01-01T00:00:00Z"))

        val auth = WgtwoAuth.builder("client-id", "client-secret")
            .oauthServer("http://127.0.0.1:" + mockServer.localPort)
            .clock(clock)
            .build()

        val tokenSource = auth.clientCredentials.newTokenSource(null)
        val callCredentials = tokenSource.callCredentials()

        val requestInfo = mockk<CallCredentials.RequestInfo>()
        val executor = MoreExecutors.directExecutor()

        run {
            val metadataApplier = MetadataApplier()
            callCredentials.applyRequestMetadata(requestInfo, executor, metadataApplier)
            assertThat(metadataApplier.metadata.get(AUTH_KEY)).isEqualTo("Bearer first")
        }

        // Advance clock 30 minutes (token not expired)
        clock += 30.minutes

        run {
            val metadataApplier = MetadataApplier()
            callCredentials.applyRequestMetadata(requestInfo, executor, metadataApplier)
            assertThat(metadataApplier.metadata.get(AUTH_KEY)).isEqualTo("Bearer first")
        }

        // Advance clock another 30 minutes to expire token (token expires after one hour)
        clock += 30.minutes

        run {
            val metadataApplier = MetadataApplier()
            callCredentials.applyRequestMetadata(requestInfo, executor, metadataApplier)
            assertThat(metadataApplier.metadata.get(AUTH_KEY)).isEqualTo("Bearer second")
        }

        // Advance clock 60 minutes to expire token (token expires after one hour)
        clock += 60.minutes

        run {
            val metadataApplier = MetadataApplier()
            callCredentials.applyRequestMetadata(requestInfo, executor, metadataApplier)
            assertThat(metadataApplier.metadata.get(AUTH_KEY)).isEqualTo("Bearer third")
        }
    }

    private fun mockServerReply(vararg tokens: String) {
        tokens.forEach { token ->
            mockServer.`when`(
                HttpRequest.request().withMethod("POST").withPath("/oauth2/token"),
                Times.exactly(1),
            ).respond(
                HttpResponse.response()
                    .withStatusCode(200)
                    .withBody(
                        """{
                          "access_token": "$token",
                          "expires_in": 3599,
                          "scope": "",
                          "token_type": "bearer"
                        }
                        """.trimIndent(),
                    )
                    .withDelay(Delay.milliseconds(250)),
            )
        }
    }

    companion object {
        private val mockServer: ClientAndServer = ClientAndServer.startClientAndServer()
    }
}

class MetadataApplier : CallCredentials.MetadataApplier() {
    var metadata = Metadata()

    override fun apply(metadata: Metadata) = this.metadata.merge(metadata)

    override fun fail(status: Status) = Unit
}

val AUTH_KEY: Metadata.Key<String> = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
