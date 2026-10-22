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
            val name = "重要タスク通知"
            val descriptionText = "未完了の重要タスクを通知バーに表示します"
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

        val mode = state.settings.notificationMode
        // 通知設定がOFFの場合は通知をキャンセルして終了
        if (mode == com.example.checklist.data.NotificationDisplayMode.OFF || !state.settings.showReqNotification) {
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
            // 未完了重要タスクがない場合は通知を消去
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        val isEnglish = state.settings.appLanguage == AppLanguage.EN
        val title = if (isEnglish) {
            "${activeReqItems.size} Pending Tasks"
        } else {
            "未完了のタスク ${activeReqItems.size}件"
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)

        if (mode == com.example.checklist.data.NotificationDisplayMode.COUNT_AND_TITLE) {
            val taskListSummary = activeReqItems.joinToString("\n") { "• ${it.title}" }
            builder.setContentText(activeReqItems.firstOrNull()?.let { "• ${it.title}" } ?: "")
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(taskListSummary)
            )
        }

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

        val schedules = state.settings.phaseSchedules
            .filter { it.phaseIndex in state.phases.indices }
            .sortedBy { it.startHour * 60 + it.startMinute }
        if (schedules.isEmpty()) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val nowDateTime = LocalDateTime.now()
        val today = nowDateTime.toLocalDate()

        // 各フェーズの終了時刻 (= 次フェーズの開始時刻) から reminderMinutes を引いた候補時刻を生成し、
        // 現在時刻より未来の最も近いリマインダー時刻を選択する
        data class ReminderCandidate(val triggerDateTime: LocalDateTime, val phaseName: String)
        val candidates = mutableListOf<ReminderCandidate>()

        for (dayOffset in 0L..1L) {
            val baseDate = today.plusDays(dayOffset)
            for (i in schedules.indices) {
                val sched = schedules[i]
                val nextSched = schedules[(i + 1) % schedules.size]
                val schedMinutes = sched.startHour * 60 + sched.startMinute
                val nextMinutes = nextSched.startHour * 60 + nextSched.startMinute
                val endDate = if (nextMinutes <= schedMinutes) baseDate.plusDays(1) else baseDate
                val endDateTime = endDate.atTime(
                    nextSched.startHour.coerceIn(0, 23),
                    nextSched.startMinute.coerceIn(0, 59)
                )
                val triggerDateTime = endDateTime.minusMinutes(reminderMinutes.toLong())
                val phaseName = state.phases.getOrNull(sched.phaseIndex)?.name ?: sched.phaseName
                candidates += ReminderCandidate(triggerDateTime, phaseName)
            }
        }

        val nextCandidate = candidates
            .filter { it.triggerDateTime.isAfter(nowDateTime.plusSeconds(5)) }
            .minByOrNull { it.triggerDateTime }
            ?: return

        val targetDateTime = nextCandidate.triggerDateTime
        val targetPhaseName = nextCandidate.phaseName

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
