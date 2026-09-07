package com.masaryamamah.otpbridge

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.text.InputType
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    companion object {
        private const val SMS_PERMISSION_REQUEST = 1001
        private const val SMS_ROLE_REQUEST = 1002
    }

    private lateinit var config: BridgeConfig
    private lateinit var brokerUrl: EditText
    private lateinit var sourceKey: EditText
    private lateinit var deviceId: EditText
    private lateinit var recipient: EditText
    private lateinit var allowlist: EditText
    private lateinit var enabled: Switch
    private lateinit var statusText: TextView
    private lateinit var platformText: TextView

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = refreshStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = BridgeConfig(this)
        setContentView(buildUi())
        loadConfig()
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(statusReceiver, IntentFilter(EventBus.ACTION_STATUS), Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(statusReceiver, IntentFilter(EventBus.ACTION_STATUS))
        }
        refreshStatus()
    }

    override fun onPause() {
        unregisterReceiver(statusReceiver)
        super.onPause()
    }

    private fun buildUi(): ScrollView {
        val pad = dp(18)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "OTP Bridge"
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Company-managed Android OTP relay for the OTP Broker"
            textSize = 14f
            setPadding(0, dp(4), 0, dp(16))
        })

        platformText = TextView(this).apply {
            setPadding(dp(12), dp(10), dp(12), dp(10))
            textSize = 13f
        }
        root.addView(platformText, matchWidth())

        brokerUrl = field("Broker URL", "https://otp.example.com")
        sourceKey = field("Source API key", "X-Source-Key", password = true)
        deviceId = field("Device ID", "OTP-PHONE-01")
        recipient = field("Recipient hint", "+9665XXXXXXXX")
        allowlist = field("Allowed SMS senders", "MUQEEM\nQIWA\nEFAA", multiLine = true)

        listOf(
            "Broker URL" to brokerUrl,
            "Source API key" to sourceKey,
            "Device ID" to deviceId,
            "Recipient hint (must match broker request, or leave blank)" to recipient,
            "Allowed senders — exact match, one per line" to allowlist
        ).forEach { (label, view) ->
            root.addView(label(label))
            root.addView(view, matchWidth())
        }

        enabled = Switch(this).apply { text = "Forward approved OTPs" }
        root.addView(enabled, matchWidth())

        val save = Button(this).apply {
            text = "Save configuration"
            setOnClickListener { saveConfig() }
        }
        root.addView(save, matchWidth())

        val permissions = Button(this).apply {
            text = "Grant SMS permission"
            setOnClickListener { requestSmsPermission() }
        }
        root.addView(permissions, matchWidth())

        val defaultSms = Button(this).apply {
            text = "Use OTP Bridge as default SMS app"
            setOnClickListener { requestSmsRole() }
        }
        root.addView(defaultSms, matchWidth())

        val test = Button(this).apply {
            text = "Test broker connection"
            setOnClickListener { testBroker() }
        }
        root.addView(test, matchWidth())

        root.addView(label("Last event"))
        statusText = TextView(this).apply {
            setPadding(dp(12), dp(12), dp(12), dp(20))
            textSize = 14f
        }
        root.addView(statusText, matchWidth())

        root.addView(TextView(this).apply {
            text = "Privacy: the app extracts the 4–8 digit code locally and sends only a synthetic OTP message to the broker. Full SMS bodies are not transmitted to the broker. A failed upload may keep only the OTP + sender encrypted for up to 2 minutes for retry."
            textSize = 12f
        })

        return ScrollView(this).apply { addView(root) }
    }

    private fun field(hintText: String, example: String, password: Boolean = false, multiLine: Boolean = false): EditText =
        EditText(this).apply {
            hint = example
            if (password) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            if (multiLine) {
                minLines = 4
                gravity = android.view.Gravity.TOP
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            }
            setPadding(dp(10), dp(6), dp(10), dp(10))
        }

    private fun label(textValue: String): TextView = TextView(this).apply {
        text = textValue
        textSize = 13f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(10), 0, 0)
    }

    private fun loadConfig() {
        brokerUrl.setText(config.brokerUrl)
        sourceKey.setText(config.sourceApiKey)
        deviceId.setText(config.deviceId)
        recipient.setText(config.recipientHint)
        allowlist.setText(config.allowlist)
        enabled.isChecked = config.forwardingEnabled
        refreshStatus()
    }

    private fun saveConfig() {
        val url = brokerUrl.text.toString().trim().trimEnd('/')
        val error = BrokerClient.validateBrokerUrl(url, BuildConfig.DEBUG)
        if (error != null) {
            toast(error)
            return
        }
        config.brokerUrl = url
        config.sourceApiKey = sourceKey.text.toString().trim()
        config.deviceId = deviceId.text.toString().trim()
        config.recipientHint = recipient.text.toString().trim()
        config.allowlist = allowlist.text.toString()
        config.forwardingEnabled = enabled.isChecked
        config.lastStatus = "Configuration saved"
        refreshStatus()
        toast("Saved")
    }

    private fun requestSmsPermission() {
        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
            toast("SMS permission is already granted")
            return
        }
        requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS), SMS_PERMISSION_REQUEST)
    }

    private fun requestSmsRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            toast("Default SMS role is managed in Android Settings on this version")
            return
        }
        val roleManager = getSystemService(RoleManager::class.java)
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
            toast("SMS role is not available on this device")
            return
        }
        if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
            toast("OTP Bridge is already the default SMS app")
            return
        }
        startActivityForResult(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS), SMS_ROLE_REQUEST)
    }

    private fun testBroker() {
        saveConfig()
        val url = config.brokerUrl
        if (url.isBlank()) return
        config.lastStatus = "Testing broker connection…"
        refreshStatus()
        Thread {
            val result = BrokerClient.health(url)
            runOnUiThread {
                config.lastStatus = if (result.ok) "Broker connected (HTTP ${result.code})" else "Broker test failed (${result.code})"
                refreshStatus()
            }
        }.start()
    }

    private fun refreshStatus() {
        if (!::statusText.isInitialized) return
        statusText.text = config.lastStatus
        val defaultPackage = Telephony.Sms.getDefaultSmsPackage(this)
        val defaultHere = defaultPackage == packageName
        val smsPermission = checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        platformText.text = when {
            Build.VERSION.SDK_INT >= 37 && !defaultHere ->
                "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}). OTP protection can delay background OTP SMS for ordinary receivers. For a dedicated company phone, select OTP Bridge as the default SMS app. Permission=${if (smsPermission) "granted" else "missing"}."
            else ->
                "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}). SMS permission=${if (smsPermission) "granted" else "missing"}; default SMS app=${if (defaultHere) "OTP Bridge" else "another app"}."
        }
    }

    private fun matchWidth() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
