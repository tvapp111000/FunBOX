package com.streamvault.data.remote.clipbox

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class ClipboxAccountState(
    val username: String? = null,
    val userId: Int? = null,
    val signedIn: Boolean = false,
    val loginRequired: Boolean = false,
)

enum class ClipboxLoginOutcome { SUCCESS, INVALID_CREDENTIALS, RATE_LIMITED, UNAVAILABLE, SECURE_STORAGE_UNAVAILABLE }

/** Account tokens are runtime sessions. Guest browsing never depends on this repository. */
@Singleton
class ClipboxAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ClipboxApi,
) {
    private val devicePreferences = context.getSharedPreferences("funbox_clipbox_device", Context.MODE_PRIVATE)
    private val _account = MutableStateFlow(readAccount())
    val account = _account.asStateFlow()

    suspend fun login(username: String, password: String): ClipboxLoginOutcome = withContext(Dispatchers.IO) {
        if (username.isBlank() || password.isBlank()) return@withContext ClipboxLoginOutcome.INVALID_CREDENTIALS
        val response = try {
            // Also establishes the server clock offset used by Clipbox's request signature.
            api.fetchConfig()
            api.login(username.trim(), password, deviceId())
        } catch (error: ClipboxAuthException) {
            return@withContext when (error.statusCode) {
                400, 401 -> ClipboxLoginOutcome.INVALID_CREDENTIALS
                429 -> ClipboxLoginOutcome.RATE_LIMITED
                else -> ClipboxLoginOutcome.UNAVAILABLE
            }
        } catch (_: Exception) {
            return@withContext ClipboxLoginOutcome.UNAVAILABLE
        }
        val stored = runCatching {
            securePreferences().edit()
                .putString("auth_token", response.token)
                .putString("auth_username", username.trim())
                .putInt("auth_user_id", response.userId)
                .commit()
        }.getOrDefault(false)
        if (!stored) return@withContext ClipboxLoginOutcome.SECURE_STORAGE_UNAVAILABLE
        _account.value = ClipboxAccountState(username.trim(), response.userId, signedIn = true)
        ClipboxLoginOutcome.SUCCESS
    }

    /** Clipbox has no observed refresh endpoint. An expired token requires a normal login. */
    suspend fun validateSession(): Boolean = withContext(Dispatchers.IO) {
        val token = sessionToken() ?: return@withContext false
        val status = runCatching {
            api.fetchConfig()
            api.checkAccount(token)
        }.getOrNull()
            ?: return@withContext _account.value.signedIn // Keep the session across transient network errors.
        if (status == 401) {
            clearSession(loginRequired = true)
            return@withContext false
        }
        status in 200..299
    }

    suspend fun logout(): Boolean = withContext(Dispatchers.IO) { clearSession(loginRequired = false) }

    /** For account synchronization only. Never log, serialize into UI state, or embed in a URL. */
    internal fun sessionToken(): String? = runCatching {
        securePreferences().getString("auth_token", null)?.takeIf(String::isNotBlank)
    }.getOrNull()

    private fun clearSession(loginRequired: Boolean): Boolean {
        val cleared = runCatching {
            securePreferences().edit()
                .remove("auth_token").remove("auth_username").remove("auth_user_id")
                .commit()
        }.getOrDefault(false)
        if (cleared || loginRequired) _account.value = ClipboxAccountState(loginRequired = loginRequired)
        return cleared
    }

    private fun readAccount(): ClipboxAccountState = runCatching {
        val prefs = securePreferences()
        val token = prefs.getString("auth_token", null)
        if (token.isNullOrBlank()) ClipboxAccountState() else ClipboxAccountState(
            username = prefs.getString("auth_username", null),
            userId = prefs.getInt("auth_user_id", -1).takeIf { it > 0 },
            signedIn = true,
        )
    }.getOrDefault(ClipboxAccountState())

    private fun deviceId(): String = synchronized(devicePreferences) {
        devicePreferences.getString("device_id", null)?.takeIf(String::isNotBlank)
            ?: UUID.randomUUID().toString().also {
                devicePreferences.edit().putString("device_id", it).commit()
            }
    }

    private fun securePreferences(): SharedPreferences {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            context.applicationContext,
            "funbox_clipbox_account",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}
