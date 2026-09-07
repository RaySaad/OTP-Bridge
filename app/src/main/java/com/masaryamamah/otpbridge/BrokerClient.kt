package com.masaryamamah.otpbridge

import android.os.Build
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

object BrokerClient {
    data class Result(val ok: Boolean, val code: Int, val message: String)

    fun validateBrokerUrl(url: String, allowDebugHttp: Boolean): String? {
        if (url.isBlank()) return "Broker URL is required"
        return try {
            val uri = URI(url)
            val scheme = uri.scheme?.lowercase()
            if (scheme == "https") null
            else if (allowDebugHttp && scheme == "http") null
            else "Release builds require HTTPS"
        } catch (_: Exception) {
            "Invalid broker URL"
        }
    }

    fun health(baseUrl: String): Result = request(
        method = "GET",
        url = "${baseUrl.trimEnd('/')}/health",
        sourceKey = null,
        body = null
    )

    fun ingest(config: BridgeConfig, sender: String, otp: String): Result {
        val source = "android:${config.deviceId}"
        val payload = buildString {
            append('{')
            append("\"recipient\":\"").append(OtpCore.jsonEscape(config.recipientHint)).append("\",")
            append("\"sender\":\"").append(OtpCore.jsonEscape(sender)).append("\",")
            // Data minimization: broker receives only a synthetic OTP message, not the original SMS body.
            append("\"message\":\"OTP ").append(OtpCore.jsonEscape(otp)).append("\",")
            append("\"source\":\"").append(OtpCore.jsonEscape(source)).append("\"")
            append('}')
        }
        return request(
            method = "POST",
            url = "${config.brokerUrl.trimEnd('/')}/v1/ingest/message",
            sourceKey = config.sourceApiKey,
            body = payload
        )
    }

    private fun request(method: String, url: String, sourceKey: String?, body: String?): Result {
        val connection = (URL(url).openConnection() as HttpURLConnection)
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 7000
            connection.readTimeout = 7000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "OTP-Bridge/0.1 Android/${Build.VERSION.SDK_INT}")
            if (!sourceKey.isNullOrBlank()) connection.setRequestProperty("X-Source-Key", sourceKey)
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = try { stream?.bufferedReader()?.use { it.readText() } ?: "" } catch (_: Exception) { "" }
            Result(code in 200..299, code, text.take(400))
        } catch (e: Exception) {
            Result(false, -1, e.javaClass.simpleName + ": " + (e.message ?: "network error"))
        } finally {
            connection.disconnect()
        }
    }
}
