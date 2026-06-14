package com.zerodrop.app

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Haptic feedback manager providing differentiated vibration patterns
 * as specified in the PRD interaction table.
 *
 * Uses VibratorManager on API 31+; falls back to the deprecated
 * getSystemService(VIBRATOR_SERVICE) on older Wear OS devices.
 */
class VibrationManager(context: Context) {

    private val vibrator: Vibrator = getVibratorCompat(context)

    /**
     * Obtain a [Vibrator] across API levels without crashing on
     * devices that lack android.os.VibratorManager (API < 31).
     */
    @Suppress("DEPRECATION")
    private fun getVibratorCompat(context: Context): Vibrator {
        // Try the API 31+ path via reflection so the class reference
        // doesn't cause NoClassDefFoundError on older devices.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val managerClass = Class.forName("android.os.VibratorManager")
                val service = context.getSystemService(managerClass)
                val defaultVibratorMethod = managerClass.getMethod("getDefaultVibrator")
                return defaultVibratorMethod.invoke(service) as Vibrator
            } catch (_: Exception) {
                // Fall through to the legacy path
            }
        }
        return context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /** 己方加分 — 短促、清脆的单次震动 */
    fun feedbackSelfScore() {
        vibrator.vibrate(
            VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    /** 对方加分 — 稍缓、沉闷的连续两次震动 */
    fun feedbackOpponentScore() {
        val timings = longArrayOf(0, 50, 100)
        val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        )
    }

    /** 撤销 — 长且连续的震动 */
    fun feedbackUndo() {
        vibrator.vibrate(
            VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    /** 进入编辑模式 — 极短的高频震动 */
    fun feedbackEditMode() {
        val timings = longArrayOf(0, 30, 60, 90)
        val amplitudes = intArrayOf(0, 100, 0, 100)
        vibrator.vibrate(
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        )
    }

    /** 局点/赛点 — 强效警示震动 */
    fun feedbackCriticalPoint() {
        val timings = longArrayOf(0, 80, 160, 240, 320)
        val amplitudes = intArrayOf(0, 255, 0, 255, 0)
        vibrator.vibrate(
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        )
    }
}