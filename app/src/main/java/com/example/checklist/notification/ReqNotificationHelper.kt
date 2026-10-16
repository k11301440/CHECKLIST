package com.example.checklist.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.checklist.R
import com.example.checklist.data.AppState
import com.example.checklist.data.Priority

object ReqNotificationHelper {
    private const val CHANNEL_ID = "req_tasks_channel"
    private const val NOTIFICATION_ID = 1001

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

        val phaseNumber = String.format("%02d", selectedIndex + 1)
        val title = "[REQ] 未完了タスク ${activeReqItems.size}件 // PHASE $phaseNumber ${currentPhase.name}"
        val text = activeReqItems.first().title

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText("${activeReqItems.size}件の必須タスク")

        activeReqItems.take(5).forEach { item ->
            inboxStyle.addLine("・${item.title}")
        }
        if (activeReqItems.size > 5) {
            inboxStyle.addLine("他 ${activeReqItems.size - 5} 件...")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancelNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
