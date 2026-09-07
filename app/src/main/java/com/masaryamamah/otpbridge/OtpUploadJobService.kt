package com.masaryamamah.otpbridge

import android.app.job.JobParameters
import android.app.job.JobService

class OtpUploadJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        Thread {
            val pending = PendingOtpStore(this).load()
            if (pending == null) {
                jobFinished(params, false)
                return@Thread
            }

            val config = BridgeConfig(this)
            if (!config.ready()) {
                config.lastStatus = "OTP detected but bridge is not fully configured"
                PendingOtpStore(this).clear()
                jobFinished(params, false)
                return@Thread
            }

            val (sender, otp) = pending
            val result = BrokerClient.ingest(config, sender, otp)
            if (result.ok) {
                PendingOtpStore(this).clear()
                config.lastStatus = "$sender OTP delivered to broker"
                EventBus.notifyStatus(this)
                jobFinished(params, false)
            } else {
                config.lastStatus = if (result.code == 404) {
                    "$sender OTP ignored: no matching waiting request"
                } else {
                    "$sender OTP delivery failed (${result.code})"
                }
                EventBus.notifyStatus(this)
                // 404 is definitive for this OTP. Other failures get a retry while the encrypted payload is <= 2 minutes old.
                if (result.code == 404 || result.code == 401 || result.code == 422) {
                    PendingOtpStore(this).clear()
                    jobFinished(params, false)
                } else {
                    jobFinished(params, true)
                }
            }
        }.start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true
}
