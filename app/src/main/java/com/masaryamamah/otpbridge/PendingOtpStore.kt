package com.masaryamamah.otpbridge

import android.content.Context

/**
 * Stores only a short-lived OTP retry payload encrypted with Android Keystore.
 * The full SMS body is never stored here.
 */
class PendingOtpStore(context: Context) {
    companion object {
        private const val PREFS = "otp_bridge_pending_meta"
        private const val SECRET_NAME = "pending_payload"
        private const val KEY_CREATED = "created_at"
        private const val MAX_AGE_MS = 2 * 60 * 1000L
    }

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val secure = SecureStore(appContext)

    fun save(sender: String, otp: String) {
        val payload = "${sender.replace("|", "")}|$otp"
        secure.putSecret(SECRET_NAME, payload)
        prefs.edit().putLong(KEY_CREATED, System.currentTimeMillis()).apply()
    }

    fun load(): Pair<String, String>? {
        val created = prefs.getLong(KEY_CREATED, 0L)
        if (created == 0L || System.currentTimeMillis() - created > MAX_AGE_MS) {
            clear()
            return null
        }
        val raw = secure.getSecret(SECRET_NAME)
        val split = raw.split('|', limit = 2)
        if (split.size != 2 || split[1].isBlank()) return null
        return split[0] to split[1]
    }

    fun clear() {
        secure.remove(SECRET_NAME)
        prefs.edit().remove(KEY_CREATED).apply()
    }
}
