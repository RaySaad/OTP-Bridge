package com.masaryamamah.otpbridge

import java.util.Locale

object OtpCore {
    private val otpRegex = Regex("(?<!\\d)(\\d{4,8})(?!\\d)")

    fun extractOtp(message: String): String? =
        otpRegex.findAll(message).lastOrNull()?.groupValues?.get(1)

    fun normalizeSender(sender: String): String =
        sender.trim().uppercase(Locale.ROOT)

    fun parseAllowlist(raw: String): Set<String> =
        raw.split('\n', ',', ';')
            .map(::normalizeSender)
            .filter { it.isNotBlank() }
            .toSet()

    fun isSenderAllowed(sender: String, allowlistRaw: String): Boolean =
        normalizeSender(sender) in parseAllowlist(allowlistRaw)

    fun jsonEscape(value: String): String {
        val out = StringBuilder(value.length + 8)
        value.forEach { c ->
            when (c) {
                '\\' -> out.append("\\\\")
                '"' -> out.append("\\\"")
                '\b' -> out.append("\\b")
                '\u000C' -> out.append("\\f")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else -> if (c.code < 0x20) out.append("\\u%04x".format(c.code)) else out.append(c)
            }
        }
        return out.toString()
    }
}
