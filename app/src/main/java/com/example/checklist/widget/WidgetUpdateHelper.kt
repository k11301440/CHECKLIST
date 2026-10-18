package com.example.checklist.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.checklist.MainActivity
import com.example.checklist.R
import com.example.checklist.data.AccentTheme
import com.example.checklist.data.AppLanguage
import com.example.checklist.data.AppSettings
import com.example.checklist.data.ChecklistRepository
import com.example.checklist.data.DarkThemeConfig
import com.example.checklist.data.Priority
import com.example.checklist.data.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime

object WidgetUpdateHelper {

    fun updateAllWidgets(context: Context, customState: com.example.checklist.data.AppState? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateAllWidgetsSync(context, customState)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateAllWidgetsSync(context: Context, customState: com.example.checklist.data.AppState? = null) {
        try {
            val state = customState ?: ChecklistRepository.getInstance(context.applicationContext).appState.value
            val appWidgetManager = AppWidgetManager.getInstance(context)

            // 1. Critical Tasks Widget
            val criticalComponent = ComponentName(context, CriticalTasksWidgetProvider::class.java)
            val criticalIds = appWidgetManager.getAppWidgetIds(criticalComponent)
            for (appWidgetId in criticalIds) {
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val views = buildTaskListWidget(context, state, isCriticalOnly = true, options = options)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            // 2. All Tasks Widget
            val allComponent = ComponentName(context, AllTasksWidgetProvider::class.java)
            val allIds = appWidgetManager.getAppWidgetIds(allComponent)
            for (appWidgetId in allIds) {
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val views = buildTaskListWidget(context, state, isCriticalOnly = false, options = options)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            // 3. Phase & Focus Widget
            val focusComponent = ComponentName(context, PhaseFocusWidgetProvider::class.java)
            val focusIds = appWidgetManager.getAppWidgetIds(focusComponent)
            for (appWidgetId in focusIds) {
                val views = buildPhaseFocusWidget(context, state)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isPinSupported(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
            appWidgetManager?.isRequestPinAppWidgetSupported == true
        } else false
    }

    fun requestPinWidget(context: Context, providerClass: Class<*>) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
            val myProvider = ComponentName(context, providerClass)
            if (appWidgetManager?.isRequestPinAppWidgetSupported == true) {
                appWidgetManager.requestPinAppWidget(myProvider, null, null)
            }
        }
    }

    private fun buildTaskListWidget(
        context: Context,
        state: com.example.checklist.data.AppState,
        isCriticalOnly: Boolean,
        options: Bundle?
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_task_list)
        val isEnglish = state.settings.appLanguage == AppLanguage.EN
        val settings = state.settings

        // Theme colors
        val (bgRes, textColor, secondaryColor, accentColor) = getThemeColors(settings)
        views.setInt(R.id.widget_root, "setBackgroundResource", bgRes)
        views.setTextColor(R.id.widget_title, textColor)
        views.setTextColor(R.id.widget_count_badge, accentColor)
        views.setTextColor(R.id.widget_big_count, accentColor)
        views.setTextColor(R.id.widget_big_label, secondaryColor)
        val isLight = settings.darkThemeConfig == DarkThemeConfig.LIGHT || settings.themeMode == ThemeMode.SPACE_WHITE
        views.setInt(R.id.widget_divider, "setBackgroundColor", when {
            isLight -> Color.parseColor("#DDDDDD")
            settings.accentTheme == AccentTheme.ANA -> Color.parseColor("#132B52")
            settings.accentTheme == AccentTheme.AIRDO -> Color.parseColor("#0B333F")
            settings.accentTheme == AccentTheme.JAL -> Color.parseColor("#401010")
            else -> Color.parseColor("#262626")
        })

        // Set title
        val titleText = if (isCriticalOnly) {
            if (isEnglish) "CRITICAL TASKS" else "重要タスク"
        } else {
            if (isEnglish) "ALL TASKS" else "全タスク"
        }
        views.setTextViewText(R.id.widget_title, titleText)

        // Gather tasks from current and active phases
        val activePhases = state.phases
        val currentIdx = state.selectedPhaseIndex.coerceIn(0, (activePhases.size - 1).coerceAtLeast(0))
        val currentPhase = activePhases.getOrNull(currentIdx)

        // Collect pending tasks (current phase first, then other phases)
        val pendingTasks = mutableListOf<com.example.checklist.data.ChecklistItem>()
        currentPhase?.items?.filter { !it.isCompleted && !it.isDeleted }?.let { items ->
            if (isCriticalOnly) {
                pendingTasks.addAll(items.filter { it.priority == Priority.REQUIRED })
            } else {
                pendingTasks.addAll(items)
            }
        }
        // If critical only, also add uncompleted required from other phases
        if (isCriticalOnly) {
            activePhases.forEachIndexed { idx, p ->
                if (idx != currentIdx) {
                    val otherReqs = p.items.filter { it.priority == Priority.REQUIRED && !it.isCompleted && !it.isDeleted }
                    pendingTasks.addAll(otherReqs)
                }
            }
        }

        val totalCount = pendingTasks.size
        views.setTextViewText(R.id.widget_count_badge, "$totalCount")
        views.setTextViewText(R.id.widget_big_count, "$totalCount")
        views.setTextViewText(R.id.widget_big_label, if (isEnglish) "PENDING" else "未完了")

        // Check widget height to decide whether to show big count or list
        val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 110
        val showCountOnly = minHeight < 100

        if (showCountOnly) {
            views.setViewVisibility(R.id.widget_big_count_container, View.VISIBLE)
            views.setViewVisibility(R.id.widget_list_container, View.GONE)
        } else {
            views.setViewVisibility(R.id.widget_big_count_container, View.GONE)
            views.setViewVisibility(R.id.widget_list_container, View.VISIBLE)

            val itemIds = listOf(
                R.id.widget_item_1,
                R.id.widget_item_2,
                R.id.widget_item_3,
                R.id.widget_item_4,
                R.id.widget_item_5
            )

            if (pendingTasks.isEmpty()) {
                views.setViewVisibility(R.id.widget_empty_text, View.VISIBLE)
                views.setTextViewText(R.id.widget_empty_text, if (isEnglish) "ALL CLEAR" else "すべて完了")
                views.setTextColor(R.id.widget_empty_text, secondaryColor)
                itemIds.forEach { views.setViewVisibility(it, View.GONE) }
            } else {
                views.setViewVisibility(R.id.widget_empty_text, View.GONE)
                itemIds.forEachIndexed { i, viewId ->
                    if (i < pendingTasks.size) {
                        val task = pendingTasks[i]
                        val prefix = if (task.priority == Priority.REQUIRED) "■ " else "□ "
                        views.setViewVisibility(viewId, View.VISIBLE)
                        views.setTextViewText(viewId, "$prefix${task.title}")
                        views.setTextColor(viewId, if (task.priority == Priority.REQUIRED) accentColor else textColor)
                    } else {
                        views.setViewVisibility(viewId, View.GONE)
                    }
                }
            }
        }

        // Tap to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            if (isCriticalOnly) 101 else 102,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        return views
    }

    private fun buildPhaseFocusWidget(
        context: Context,
        state: com.example.checklist.data.AppState
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_phase_focus)
        val isEnglish = state.settings.appLanguage == AppLanguage.EN
        val settings = state.settings

        val (bgRes, textColor, secondaryColor, accentColor) = getThemeColors(settings)
        views.setInt(R.id.widget_focus_root, "setBackgroundResource", bgRes)
        views.setTextColor(R.id.widget_focus_phase, textColor)
        views.setTextColor(R.id.widget_focus_time_remaining, secondaryColor)
        views.setTextColor(R.id.widget_focus_label, accentColor)
        views.setTextColor(R.id.widget_focus_task_title, textColor)
        views.setTextColor(R.id.widget_focus_subinfo, secondaryColor)
        val isLight = settings.darkThemeConfig == DarkThemeConfig.LIGHT || settings.themeMode == ThemeMode.SPACE_WHITE
        views.setInt(R.id.widget_focus_divider, "setBackgroundColor", when {
            isLight -> Color.parseColor("#DDDDDD")
            settings.accentTheme == AccentTheme.ANA -> Color.parseColor("#132B52")
            settings.accentTheme == AccentTheme.AIRDO -> Color.parseColor("#0B333F")
            settings.accentTheme == AccentTheme.JAL -> Color.parseColor("#401010")
            else -> Color.parseColor("#262626")
        })

        val activePhases = state.phases
        val currentIdx = state.selectedPhaseIndex.coerceIn(0, (activePhases.size - 1).coerceAtLeast(0))
        val currentPhase = activePhases.getOrNull(currentIdx)

        val phaseNumber = String.format("%02d", currentIdx + 1)
        val phaseName = currentPhase?.name ?: "Phase"
        views.setTextViewText(R.id.widget_focus_phase, "PHASE $phaseNumber $phaseName")

        // Calculate time remaining until next phase
        val schedules = state.settings.phaseSchedules
        val timeRemainingStr = if (schedules.isNotEmpty()) {
            val nextSchedule = schedules.find { it.phaseIndex == (currentIdx + 1) % schedules.size } ?: schedules.first()
            val now = LocalTime.now()
            val currentMinutes = now.hour * 60 + now.minute
            val nextMinutes = nextSchedule.startHour * 60 + nextSchedule.startMinute
            val diffMinutes = if (nextMinutes > currentMinutes) {
                nextMinutes - currentMinutes
            } else {
                (24 * 60 - currentMinutes) + nextMinutes
            }
            val hoursLeft = diffMinutes / 60
            val minsLeft = diffMinutes % 60
            if (hoursLeft > 0) {
                if (isEnglish) "${hoursLeft}h ${minsLeft}m left" else "残り ${hoursLeft}時間${minsLeft}分"
            } else {
                if (isEnglish) "${minsLeft}m left" else "残り ${minsLeft}分"
            }
        } else {
            ""
        }
        views.setTextViewText(R.id.widget_focus_time_remaining, timeRemainingStr)

        // Find topmost critical task, or first uncompleted task (Focus Target)
        val activeItems = currentPhase?.items?.filter { !it.isCompleted && !it.isDeleted } ?: emptyList()
        val topCritical = activeItems.firstOrNull { it.priority == Priority.REQUIRED }
        val focusTask = topCritical ?: activeItems.firstOrNull()

        if (focusTask != null) {
            views.setTextViewText(
                R.id.widget_focus_label,
                if (focusTask.priority == Priority.REQUIRED) {
                    if (isEnglish) "TARGET // CRITICAL" else "最優先ターゲット // 重要"
                } else {
                    if (isEnglish) "TARGET // STANDARD" else "現在ターゲット // 標準"
                }
            )
            views.setTextViewText(R.id.widget_focus_task_title, focusTask.title)
        } else {
            views.setTextViewText(R.id.widget_focus_label, if (isEnglish) "STATUS" else "状態")
            views.setTextViewText(
                R.id.widget_focus_task_title,
                if (isEnglish) "All phase tasks complete" else "このフェーズのタスクはすべて完了"
            )
        }

        val remainingReq = activeItems.count { it.priority == Priority.REQUIRED }
        val remainingNorm = activeItems.count { it.priority == Priority.NORMAL }
        val subInfoText = if (isEnglish) {
            "Remaining: $remainingReq Critical • $remainingNorm Standard"
        } else {
            "残り: 重要 $remainingReq 件 • 標準 $remainingNorm 件"
        }
        views.setTextViewText(R.id.widget_focus_subinfo, subInfoText)

        // Tap to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            103,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_focus_root, pendingIntent)

        return views
    }

    private data class ThemeWidgetColors(
        val bgDrawable: Int,
        val textPrimary: Int,
        val textSecondary: Int,
        val accentColor: Int
    )

    private fun getThemeColors(settings: AppSettings): ThemeWidgetColors {
        val isLight = settings.darkThemeConfig == DarkThemeConfig.LIGHT || settings.themeMode == ThemeMode.SPACE_WHITE
        return when (settings.accentTheme) {
            AccentTheme.MONOCHROME -> {
                if (isLight) {
                    ThemeWidgetColors(
                        bgDrawable = R.drawable.widget_bg_white,
                        textPrimary = Color.BLACK,
                        textSecondary = Color.parseColor("#666666"),
                        accentColor = Color.BLACK
                    )
                } else {
                    ThemeWidgetColors(
                        bgDrawable = R.drawable.widget_bg_black,
                        textPrimary = Color.WHITE,
                        textSecondary = Color.parseColor("#888888"),
                        accentColor = Color.WHITE
                    )
                }
            }
            AccentTheme.TRON -> ThemeWidgetColors(
                bgDrawable = if (isLight) R.drawable.widget_bg_white else R.drawable.widget_bg_tron,
                textPrimary = if (isLight) Color.parseColor("#C62828") else Color.WHITE,
                textSecondary = if (isLight) Color.parseColor("#E53935") else Color.parseColor("#888888"),
                accentColor = Color.parseColor("#FFFF1E27")
            )
            AccentTheme.MONSTER -> ThemeWidgetColors(
                bgDrawable = if (isLight) R.drawable.widget_bg_white else R.drawable.widget_bg_monster,
                textPrimary = if (isLight) Color.parseColor("#1B5E20") else Color.WHITE,
                textSecondary = if (isLight) Color.parseColor("#2E7D32") else Color.parseColor("#888888"),
                accentColor = if (isLight) Color.parseColor("#FF00A844") else Color.parseColor("#FF00FF41")
            )
            AccentTheme.ANA -> ThemeWidgetColors(
                bgDrawable = if (isLight) R.drawable.widget_bg_white else R.drawable.widget_bg_ana,
                textPrimary = if (isLight) Color.parseColor("#002D62") else Color.parseColor("#E1EFFF"),
                textSecondary = if (isLight) Color.parseColor("#0056B3") else Color.parseColor("#80B3FF"),
                accentColor = Color.parseColor("#FF1A8CFF")
            )
            AccentTheme.AIRDO -> ThemeWidgetColors(
                bgDrawable = if (isLight) R.drawable.widget_bg_white else R.drawable.widget_bg_airdo,
                textPrimary = if (isLight) Color.parseColor("#004D40") else Color.parseColor("#E0F7FA"),
                textSecondary = if (isLight) Color.parseColor("#00838F") else Color.parseColor("#80DEEA"),
                accentColor = Color.parseColor("#FFFFD200")
            )
            AccentTheme.JAL -> ThemeWidgetColors(
                bgDrawable = if (isLight) R.drawable.widget_bg_white else R.drawable.widget_bg_jal,
                textPrimary = if (isLight) Color.parseColor("#61000A") else Color.parseColor("#FFFFEEEE"),
                textSecondary = if (isLight) Color.parseColor("#B71C1C") else Color.parseColor("#FFFF7A7A"),
                accentColor = Color.parseColor("#FFE60012")
            )
        }
    }
}
