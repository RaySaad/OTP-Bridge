package com.masaryamamah.otpbridge

import android.content.Context
import android.content.Intent

object EventBus {
    const val ACTION_STATUS = "com.masaryamamah.otpbridge.STATUS_CHANGED"
    fun notifyStatus(context: Context) {
        context.sendBroadcast(Intent(ACTION_STATUS).setPackage(context.packageName))
    }
}
