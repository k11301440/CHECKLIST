package com.example.checklist.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.checklist.MainActivity
import com.example.checklist.R
import com.example.checklist.data.AppLanguage

class PhaseReminderReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "phase_reminder_channel"
        const val NOTIFICATION_ID = 1002
        const val EXTRA_PHASE_NAME = "extra_phase_name"
        const val EXTRA_MINUTES_LEFT = "extra_minutes_left"
        const val EXTRA_IS_ENGLISH = "extra_is_english"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Phase End Reminder"
                val descriptionText = "フェーズ終了前のリマインダー通知"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableVibration(true)
                }
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        createNotificationChannel(context)

        val phaseName = intent.getStringExtra(EXTRA_PHASE_NAME) ?: "Current Phase"
        val minutesLeft = intent.getIntExtra(EXTRA_MINUTES_LEFT, 30)
        val isEnglish = intent.getBooleanExtra(EXTRA_IS_ENGLISH, true)

        val title = if (isEnglish) {
            "PHASE END IN $minutesLeft MINS"
        } else {
            "フェーズ終了まであと $minutesLeft 分"
        }

        val message = if (isEnglish) {
            "Phase '$phaseName' will end soon. Please review remaining checklist tasks."
        } else {
            "フェーズ「$phaseName」が終了します。未完了タスクを確認してください。"
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
