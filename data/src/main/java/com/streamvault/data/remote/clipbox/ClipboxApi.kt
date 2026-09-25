package com.streamvault.data.remote.clipbox

import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** Values are injected at build time; none of the APK-derived values live in source control. */
data class ClipboxCredentials(
    val appKey: String,
    val signingDigest: String,
    val appVersion: Int = 161,
) {
    val isConfigured: Boolean get() = appKey.isNotBlank() && signingDigest.matches(Regex("[0-9A-Fa-f]{64}"))
}

data class ClipboxRemoteConfig(
    val tmdbKey: String,
    val serverTimeMillis: Long,
    val maintenanceMessage: String,
)

data class ClipboxLoginResult(val token: String, val userId: Int)

class ClipboxAuthException(val statusCode: Int) : Exception("Clipbox authentication failed (HTTP $statusCode)")

/** The original Clipbox service gate. It sends credentials only to APK-listed HTTPS hosts. */
class ClipboxApi(
    private val credentials: ClipboxCredentials,
    private val clockMillis: () -> Long = System::currentTimeMillis,
    client: OkHttpClient = OkHttpClient.Builder().build(),
) {
    private val client = client.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val hosts = listOf("clipbox.mov", "api.clipbox.mov", "superbox.mov")
    private var serverOffsetMillis: Long = 0L
    @Volatile private var activeHost: String = hosts.first()

    suspend fun fetchConfig(): ClipboxRemoteConfig = withContext(Dispatchers.IO) {
        require(credentials.isConfigured) { "Clipbox build credentials are missing" }
        var lastFailure: Exception? = null
        for (host in hosts) {
            try {
                val bootstrap = request(host, "/api/config", signed = false)
                val serverTime = bootstrap.optLong("server_time", 0L)
                if (serverTime > 0L) serverOffsetMillis = serverTime - clockMillis()
                val config = request(host, "/api/config", signed = true)
                activeHost = host
                return@withContext ClipboxRemoteConfig(
                    tmdbKey = config.optString("tmdb_api_key", ""),
                    serverTimeMillis = config.optLong("server_time", serverTime),
                    maintenanceMessage = config.optString("maintenance_message", ""),
                )
            } catch (error: Exception) {
                lastFailure = error
            }
        }
        throw IllegalStateException("Clipbox configuration is unavailable", lastFailure)
    }

    /** Returns the endpoint response without treating a missing user token as an app-signature failure. */
    suspend fun checkAccount(token: String?): Int = withContext(Dispatchers.IO) {
        require(credentials.isConfigured) { "Clipbox build credentials are missing" }
        val request = signedRequest(activeHost, "/api/auth/me", token)
        client.newCall(request).execute().use { it.code }
    }

    /** The APK's account login contract; no password or token is logged or cached here. */
    suspend fun login(username: String, password: String, deviceId: String): ClipboxLoginResult =
        withContext(Dispatchers.IO) {
            require(credentials.isConfigured) { "Clipbox build credentials are missing" }
            require(username.isNotBlank() && password.isNotBlank() && deviceId.isNotBlank())
            val body = JSONObject().apply {
                put("username", username.trim())
                put("password", password)
                put("deviceId", deviceId)
            }.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = signedRequest(activeHost, "/api/auth/login", null, body)
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw ClipboxAuthException(response.code)
                val json = JSONObject(response.body?.string().orEmpty())
                val token = json.optString("token")
                val userId = json.optInt("userId", -1)
                check(token.isNotBlank() && userId > 0) { "Clipbox login response is incomplete" }
                ClipboxLoginResult(token, userId)
            }
        }

    private fun request(host: String, path: String, signed: Boolean): JSONObject {
        val request = if (signed) signedRequest(host, path, null) else Request.Builder()
            .url("https://$host$path")
            .header("User-Agent", USER_AGENT)
            .header("X-App-Version", credentials.appVersion.toString())
            .header("X-App-Key", credentials.appKey)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Clipbox HTTP ${response.code}")
            return JSONObject(response.body?.string().orEmpty())
        }
    }

    private fun signedRequest(host: String, path: String, token: String?, body: RequestBody? = null): Request {
        require(host in hosts && path.startsWith("/api/"))
        val minute = (clockMillis() + serverOffsetMillis) / 60_000L
        val payload = "${credentials.signingDigest.uppercase()}|${credentials.appVersion}|$minute"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(credentials.appKey.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val signature = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return Request.Builder()
            .url("https://$host$path")
            .method(if (body == null) "GET" else "POST", body)
            .header("User-Agent", USER_AGENT)
            .header("X-App-Version", credentials.appVersion.toString())
            .header("X-App-Key", credentials.appKey)
            .header("X-App-Integrity", signature)
            .apply { token?.takeIf(String::isNotBlank)?.let { header("Authorization", "Bearer $it") } }
            .build()
    }

    private companion object {
        const val USER_AGENT = "SuperBoxMobileApp/1.0"
    }
}
