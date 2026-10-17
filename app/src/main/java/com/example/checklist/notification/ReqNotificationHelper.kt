package com.example.checklist.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.checklist.MainActivity
import com.example.checklist.R
import com.example.checklist.data.AppLanguage
import com.example.checklist.data.AppState
import com.example.checklist.data.Priority
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar

object ReqNotificationHelper {
    private const val CHANNEL_ID = "req_tasks_channel"
    private const val NOTIFICATION_ID = 1001
    private const val REMINDER_ALARM_REQ_CODE = 2001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "REQUIRED Tasks"
            val descriptionText = "未完了の必須(REQ)タスクを通知バーに表示します"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        PhaseReminderReceiver.createNotificationChannel(context)
    }

    fun updateNotification(context: Context, state: AppState) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 通知設定がOFFの場合は通知をキャンセルして終了
        if (!state.settings.showReqNotification) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        // Android 13+ の権限確認
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                return
            }
        }

        val selectedIndex = state.selectedPhaseIndex.coerceIn(0, (state.phases.size - 1).coerceAtLeast(0))
        val currentPhase = state.phases.getOrNull(selectedIndex) ?: return

        val activeReqItems = currentPhase.items.filter {
            it.priority == Priority.REQUIRED && !it.isCompleted && !it.isDeleted
        }

        if (activeReqItems.isEmpty()) {
            // 未完了REQタスクがない場合は通知を消去
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        val isEnglish = state.settings.appLanguage == AppLanguage.EN
        val phaseNumber = String.format("%02d", selectedIndex + 1)
        val title = if (isEnglish) {
            "[REQ] ${activeReqItems.size} PENDING TASKS // PHASE $phaseNumber ${currentPhase.name}"
        } else {
            "[REQ] 未完了タスク ${activeReqItems.size}件 // PHASE $phaseNumber ${currentPhase.name}"
        }
        val text = activeReqItems.first().title

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText(if (isEnglish) "${activeReqItems.size} required tasks" else "${activeReqItems.size}件の必須タスク")

        activeReqItems.take(5).forEach { item ->
            inboxStyle.addLine("・${item.title}")
        }
        if (activeReqItems.size > 5) {
            inboxStyle.addLine(if (isEnglish) "+${activeReqItems.size - 5} more..." else "他 ${activeReqItems.size - 5} 件...")
        }

        // タップ時にアプリを開く PendingIntent
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
            .setContentText(text)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun schedulePhaseReminder(context: Context, state: AppState) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val reminderMinutes = state.settings.phaseEndReminderMinutes

        val intent = Intent(context, PhaseReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (reminderMinutes <= 0 || state.phases.isEmpty() || state.settings.phaseSchedules.isEmpty()) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val schedules = state.settings.phaseSchedules.sortedBy { it.phaseIndex }
        val now = LocalTime.now()
        val nowMinutes = now.hour * 60 + now.minute

        // 現在のフェーズの終了時刻＝次のフェーズの開始時刻（夜フェーズなら翌日の最初のフェーズまたは24:00）
        var targetEndMinutes: Int = -1
        var targetPhaseName = ""

        for (i in schedules.indices) {
            val sched = schedules[i]
            val schedMinutes = sched.startHour * 60 + sched.startMinute
            val nextSched = schedules.getOrNull(i + 1) ?: schedules.first()
            val nextSchedMinutes = if (i + 1 < schedules.size) {
                nextSched.startHour * 60 + nextSched.startMinute
            } else {
                // 最後のフェーズの終了は翌日の第1フェーズ開始 (または 24:00)
                (nextSched.startHour * 60 + nextSched.startMinute) + 24 * 60
            }

            val isInThisPhase = if (i + 1 < schedules.size) {
                nowMinutes >= schedMinutes && nowMinutes < nextSchedMinutes
            } else {
                nowMinutes >= schedMinutes || nowMinutes < (schedules.first().startHour * 60 + schedules.first().startMinute)
            }

            if (isInThisPhase) {
                targetPhaseName = sched.phaseName
                targetEndMinutes = nextSchedMinutes
                break
            }
        }

        if (targetEndMinutes == -1) {
            targetEndMinutes = (schedules.first().startHour * 60 + schedules.first().startMinute) + 24 * 60
            targetPhaseName = schedules.lastOrNull()?.phaseName ?: "Current Phase"
        }

        // リマインダー通知時刻（終了の reminderMinutes 前）
        var triggerMinutes = targetEndMinutes - reminderMinutes
        val today = LocalDate.now()
        var targetDateTime: LocalDateTime

        if (triggerMinutes > nowMinutes) {
            val hour = (triggerMinutes % (24 * 60)) / 60
            val minute = triggerMinutes % 60
            val daysToAdd = (triggerMinutes / (24 * 60)).toLong()
            targetDateTime = today.plusDays(daysToAdd).atTime(hour, minute)
        } else {
            // すでに今のフェーズのリマインダー時刻を過ぎている場合は、次のフェーズ終了前リマインダーをセット
            val nextEndMinutes = targetEndMinutes + (3 * 60) // 概算次フェーズ
            val hour = (nextEndMinutes % (24 * 60)) / 60
            val minute = nextEndMinutes % 60
            val daysToAdd = (nextEndMinutes / (24 * 60)).toLong()
            targetDateTime = today.plusDays(daysToAdd).atTime(hour, minute)
        }

        val triggerEpochMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val reminderIntent = Intent(context, PhaseReminderReceiver::class.java).apply {
            putExtra(PhaseReminderReceiver.EXTRA_PHASE_NAME, targetPhaseName)
            putExtra(PhaseReminderReceiver.EXTRA_MINUTES_LEFT, reminderMinutes)
            putExtra(PhaseReminderReceiver.EXTRA_IS_ENGLISH, state.settings.appLanguage == AppLanguage.EN)
        }
        val reminderPendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_ALARM_REQ_CODE,
            reminderIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerEpochMillis,
                reminderPendingIntent
            )
        } catch (e: Exception) {
            // AlarmManager fallback
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerEpochMillis,
                reminderPendingIntent
            )
        }
    }

    fun cancelNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
