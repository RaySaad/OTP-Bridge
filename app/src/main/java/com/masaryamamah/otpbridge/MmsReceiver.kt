package com.masaryamamah.otpbridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // OTP Bridge intentionally does not process MMS content.
        BridgeConfig(context).lastStatus = "MMS received; ignored by OTP Bridge"
        EventBus.notifyStatus(context)
    }
}
