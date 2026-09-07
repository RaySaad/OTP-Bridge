package com.masaryamamah.otpbridge

import android.content.Context
import java.util.UUID

class BridgeConfig(context: Context) {
    companion object {
        private const val PREFS = "otp_bridge_settings"
        private const val KEY_BROKER_URL = "broker_url"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_RECIPIENT = "recipient_hint"
        private const val KEY_ALLOWLIST = "allowlist"
        private const val KEY_ENABLED = "forwarding_enabled"
        private const val KEY_LAST_STATUS = "last_status"
        private const val KEY_SOURCE_KEY = "source_api_key"
    }

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val secure = SecureStore(appContext)

    var brokerUrl: String
        get() = prefs.getString(KEY_BROKER_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BROKER_URL, value.trim().trimEnd('/')).apply()

    var deviceId: String
        get() {
            val current = prefs.getString(KEY_DEVICE_ID, "") ?: ""
            if (current.isNotBlank()) return current
            val generated = "OTP-PHONE-${UUID.randomUUID().toString().take(8).uppercase()}"
            prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
            return generated
        }
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value.trim()).apply()

    var recipientHint: String
        get() = prefs.getString(KEY_RECIPIENT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_RECIPIENT, value.trim()).apply()

    var allowlist: String
        get() = prefs.getString(KEY_ALLOWLIST, "MUQEEM\nQIWA\nEFAA") ?: ""
        set(value) = prefs.edit().putString(KEY_ALLOWLIST, value).apply()

    var forwardingEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var lastStatus: String
        get() = prefs.getString(KEY_LAST_STATUS, "Not configured") ?: "Not configured"
        set(value) = prefs.edit().putString(KEY_LAST_STATUS, value).apply()

    var sourceApiKey: String
        get() = secure.getSecret(KEY_SOURCE_KEY)
        set(value) = secure.putSecret(KEY_SOURCE_KEY, value)

    fun ready(): Boolean =
        forwardingEnabled && brokerUrl.isNotBlank() && sourceApiKey.isNotBlank() && OtpCore.parseAllowlist(allowlist).isNotEmpty()
}
