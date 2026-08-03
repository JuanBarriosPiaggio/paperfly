package com.paperfly.paperplanedrift.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticsManager(context: Context) {

    @Volatile
    var enabled: Boolean = true

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun crash() = vibrate(60, 180)
    fun lightTap() = vibrate(18, 80)

    private fun vibrate(ms: Long, amplitude: Int) {
        if (!enabled) return
        runCatching {
            vibrator?.vibrate(VibrationEffect.createOneShot(ms, amplitude))
        }
    }
}
