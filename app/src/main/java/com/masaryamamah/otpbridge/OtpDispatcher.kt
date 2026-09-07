package com.masaryamamah.otpbridge

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context

object OtpDispatcher {
    private const val JOB_ID = 45021

    fun handleMessage(context: Context, senderRaw: String, message: String) {
        val config = BridgeConfig(context)
        val sender = OtpCore.normalizeSender(senderRaw)

        if (!config.forwardingEnabled) {
            config.lastStatus = "$sender SMS received; forwarding is OFF"
            EventBus.notifyStatus(context)
            return
        }
        if (!OtpCore.isSenderAllowed(sender, config.allowlist)) {
            config.lastStatus = "$sender SMS ignored: sender not allowlisted"
            EventBus.notifyStatus(context)
            return
        }
        val otp = OtpCore.extractOtp(message)
        if (otp == null) {
            config.lastStatus = "$sender SMS ignored: no 4-8 digit OTP detected"
            EventBus.notifyStatus(context)
            return
        }

        PendingOtpStore(context).save(sender, otp)
        config.lastStatus = "$sender OTP detected; queued for broker"
        EventBus.notifyStatus(context)

        val scheduler = context.getSystemService(JobScheduler::class.java)
        val job = JobInfo.Builder(JOB_ID, ComponentName(context, OtpUploadJobService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setMinimumLatency(0)
            .setOverrideDeadline(10_000)
            .build()
        scheduler.schedule(job)
    }
}
