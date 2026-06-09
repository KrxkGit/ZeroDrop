package com.zerodrop.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status

/**
 * Manages Wear OS Ongoing Activity API integration.
 *
 * When a match is in progress, registering an [OngoingActivity] tells
 * Wear OS that the app is actively recording scores:
 *  - Keeps the app alive (reduces kill probability)
 *  - Shows a persistent indicator on the watch face
 *  - Supports auto-resume on wrist-raise during the match
 */
class OngoingActivityManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "zerodrop_ongoing_match"
        private const val NOTIFICATION_ID = 1001
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val openIntent: PendingIntent by lazy {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private var started = false

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.ongoing_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.ongoing_channel_description)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /** Register or refresh the "match in progress" status on the watch. */
    fun updateMatchStatus(leftScore: Int, rightScore: Int, setNum: Int) {
        val scoreText = "Set $setNum: $leftScore - $rightScore"

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ongoing_notification)
            .setContentTitle(context.getString(R.string.ongoing_notification_title))
            .setContentText(scoreText)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        val status = Status.Builder()
            .addTemplate("#score#")
            .addPart("score", Status.TextPart(scoreText))
            .build()

        val ongoingActivity = OngoingActivity.Builder(
            context,
            NOTIFICATION_ID,
            notificationBuilder
        )
            .setStaticIcon(R.drawable.ic_ongoing_notification)
            .setStatus(status)
            .build()

        ongoingActivity.apply(context)
        started = true
    }

    /** Remove the ongoing status (match ended). */
    fun stopMatch() {
        if (started) {
            notificationManager.cancel(NOTIFICATION_ID)
            started = false
        }
    }
}