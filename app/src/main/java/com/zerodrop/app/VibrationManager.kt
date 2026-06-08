package com.zerodrop.app

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Haptic feedback manager providing differentiated vibration patterns
 * as specified in the PRD interaction table.
 */
class VibrationManager(context: Context) {

    private val vibrator: Vibrator = run {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
            ?: context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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
