package com.streamvault.data.remote.clipbox

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ClipboxApiTest {
    @Test
    fun loginUsesSignedOriginalHostAndParsesRuntimeToken() = runBlocking {
        var observedMethod = ""
        var observedHost = ""
        var observedBody = JSONObject()
        var signatureLength = 0
        val interceptor = Interceptor { chain ->
            val request = chain.request()
            observedMethod = request.method
            observedHost = request.url.host
            signatureLength = request.header("X-App-Integrity")?.length ?: 0
            val buffer = okio.Buffer()
            request.body?.writeTo(buffer)
            observedBody = JSONObject(buffer.readUtf8())
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK")
                .body("""{"token":"test-runtime-token","userId":17}""".toResponseBody())
                .build()
        }
        val api = ClipboxApi(
            ClipboxCredentials("unit-test-key", "A".repeat(64)),
            clockMillis = { 1_700_000_000_000L },
            client = OkHttpClient.Builder().addInterceptor(interceptor).build(),
        )

        val result = api.login(" viewer ", "password", "device-uuid")

        assertThat(observedMethod).isEqualTo("POST")
        assertThat(observedHost).isEqualTo("clipbox.mov")
        assertThat(signatureLength).isEqualTo(64)
        assertThat(observedBody.getString("username")).isEqualTo("viewer")
        assertThat(observedBody.getString("password")).isEqualTo("password")
        assertThat(observedBody.getString("deviceId")).isEqualTo("device-uuid")
        assertThat(result.userId).isEqualTo(17)
        assertThat(result.token).isEqualTo("test-runtime-token")
    }

    @Test
    fun loginDoesNotTreatUnauthorizedAsSuccessfulSession() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(401).message("Unauthorized").body("".toResponseBody()).build()
        }.build()
        val api = ClipboxApi(ClipboxCredentials("unit-test-key", "A".repeat(64)), client = client)

        val failure = runCatching { api.login("viewer", "password", "device-uuid") }.exceptionOrNull()

        assertThat(failure).isInstanceOf(ClipboxAuthException::class.java)
        assertThat((failure as ClipboxAuthException).statusCode).isEqualTo(401)
    }
}
