package com.example.checklist.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.checklist.data.AppLanguage
import com.example.checklist.data.AppState
import com.example.checklist.data.Phase
import com.example.checklist.data.PhaseTemplate
import com.example.checklist.data.ThemeMode
import com.example.checklist.ui.common.AppStrings
import com.example.checklist.theme.BorderProminent
import com.example.checklist.theme.BorderSubtle
import com.example.checklist.theme.SpaceBlack
import com.example.checklist.theme.SurfaceDark
import com.example.checklist.theme.SurfaceElevated
import com.example.checklist.theme.TextDisabled
import com.example.checklist.theme.TextPrimary
import com.example.checklist.theme.TextSecondary
import com.example.checklist.theme.TextTertiary
import com.example.checklist.ui.ChecklistViewModel
import com.example.checklist.ui.components.SpaceXButton
import com.example.checklist.ui.components.SpaceXConfirmDialog
import com.example.checklist.ui.components.SpaceXDangerButton
import com.example.checklist.ui.components.SpaceXDivider
import com.example.checklist.ui.components.SpaceXShape
import com.example.checklist.ui.components.SpaceXSmallShape
import com.example.checklist.ui.components.SpaceXTextButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    state: AppState,
    viewModel: ChecklistViewModel,
    onBack: () -> Unit,
    onOpenTemplateEditor: (templateId: String?) -> Unit,
    onNavigatePrivacyPolicy: () -> Unit = {},
    onNavigateUserGuide: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setShowReqNotification(true)
        } else {
            viewModel.setShowReqNotification(false)
        }
    }

    var editingPhase by remember { mutableStateOf<Phase?>(null) }
    var showAddPhaseDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<com.example.checklist.data.PhaseSchedule?>(null) }
    var showResetDefaultConfirm by remember { mutableStateOf(false) }
    var showResetAllDataConfirm by remember { mutableStateOf(false) }
    var showReleaseNotesDialog by remember { mutableStateOf(false) }
    var showAviationTipsDialog by remember { mutableStateOf(false) }
    val lang = state.settings.appLanguage
    val isEnglish = lang == AppLanguage.EN

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = AppStrings.settings(lang),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.size(36.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 0. LANGUAGE (日・英 表記切り替え)
            item {
                SettingsSectionHeader(AppStrings.sectionLanguage(lang))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOptionButton(
                        text = "ENGLISH",
                        subtext = "EN",
                        isSelected = lang == AppLanguage.EN,
                        onClick = { viewModel.setAppLanguage(AppLanguage.EN) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionButton(
                        text = "日本語",
                        subtext = if (lang == AppLanguage.EN) "JA (DEFAULT)" else "JA (デフォルト)",
                        isSelected = lang == AppLanguage.JA,
                        onClick = { viewModel.setAppLanguage(AppLanguage.JA) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 1. THEME MODE (白黒・トロン赤・モンスター緑)
            item {
                SettingsSectionHeader(AppStrings.sectionTheme(lang))
                Spacer(modifier = Modifier.height(8.dp))

                val currentMode = state.settings.themeMode
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionButton(
                            text = "SPACE BLACK",
                            subtext = if (isEnglish) "MINIMAL MONO" else "純黒モノクロ",
                            isSelected = currentMode == ThemeMode.SPACE_BLACK,
                            onClick = { viewModel.setThemeMode(ThemeMode.SPACE_BLACK) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionButton(
                            text = "SPACE WHITE",
                            subtext = if (isEnglish) "LIGHT MODE" else "ライトモード",
                            isSelected = currentMode == ThemeMode.SPACE_WHITE,
                            onClick = { viewModel.setThemeMode(ThemeMode.SPACE_WHITE) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionButton(
                            text = "TRON RED",
                            subtext = if (isEnglish) "CYBER RED" else "トロン (赤外縁)",
                            isSelected = currentMode == ThemeMode.TRON_RED,
                            onClick = { viewModel.setThemeMode(ThemeMode.TRON_RED) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionButton(
                            text = "MONSTER",
                            subtext = if (isEnglish) "ENERGY GREEN" else "モンスター (緑外縁)",
                            isSelected = currentMode == ThemeMode.MONSTER_GREEN,
                            onClick = { viewModel.setThemeMode(ThemeMode.MONSTER_GREEN) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. MISSION AUTOMATION (時間自動進行 & REQ/NORMキャリーオーバー & 完了確認)
            item {
                SettingsSectionHeader(AppStrings.sectionAutomation(lang))
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, SpaceXShape)
                        .background(SurfaceDark, shape = SpaceXShape)
                ) {
                    // 自動進行トグル
                    SettingToggleRow(
                        title = AppStrings.autoAdvance(lang),
                        description = AppStrings.autoAdvanceDesc(lang),
                        enabled = state.settings.autoAdvanceByTime,
                        onToggle = { viewModel.setAutoAdvanceByTime(!state.settings.autoAdvanceByTime) }
                    )

                    SpaceXDivider()

                    // 未完了REQタスク持ち越しトグル
                    SettingToggleRow(
                        title = AppStrings.carryoverReq(lang),
                        description = AppStrings.carryoverReqDesc(lang),
                        enabled = state.settings.carryoverUnfinishedReq,
                        onToggle = { viewModel.setCarryoverUnfinishedReq(!state.settings.carryoverUnfinishedReq) }
                    )

                    SpaceXDivider()

                    // 未完了NORMタスク持ち越しトグル (フィードバック対応)
                    SettingToggleRow(
                        title = AppStrings.carryoverNorm(lang),
                        description = AppStrings.carryoverNormDesc(lang),
                        enabled = state.settings.carryoverUnfinishedNorm,
                        onToggle = { viewModel.setCarryoverUnfinishedNorm(!state.settings.carryoverUnfinishedNorm) }
                    )

                    SpaceXDivider()

                    // フェーズ完了時の確認トグル
                    SettingToggleRow(
                        title = if (isEnglish) "Confirm review on phase advance" else "フェーズ進行時の確認",
                        description = if (isEnglish) "Show evaluation dialog before advancing" else "次のフェーズへ進む際に完了確認ダイアログを表示する",
                        enabled = state.settings.confirmReviewOnAdvance,
                        onToggle = { viewModel.setConfirmReviewOnAdvance(!state.settings.confirmReviewOnAdvance) }
                    )

                    SpaceXDivider()

                    // 未完了REQタスク通知トグル (オプトイン)
                    SettingToggleRow(
                        title = AppStrings.reqNotification(lang),
                        description = AppStrings.reqNotificationDesc(lang),
                        enabled = state.settings.showReqNotification,
                        onToggle = {
                            val targetState = !state.settings.showReqNotification
                            if (targetState) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!hasPermission) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.setShowReqNotification(true)
                                    }
                                } else {
                                    viewModel.setShowReqNotification(true)
                                }
                            } else {
                                viewModel.setShowReqNotification(false)
                            }
                        }
                    )

                    SpaceXDivider()

                    // フェーズ終了前リマインダー通知 (設定画面 4: 任意の時間前にリマインダー)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = AppStrings.phaseEndReminder(lang),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = AppStrings.phaseEndReminderDesc(lang),
                                color = TextSecondary,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        }

                        val currentMinutes = state.settings.phaseEndReminderMinutes
                        val reminderLabel = when (currentMinutes) {
                            10 -> if (isEnglish) "10 MIN" else "10分前"
                            15 -> if (isEnglish) "15 MIN" else "15分前"
                            30 -> if (isEnglish) "30 MIN" else "30分前"
                            60 -> if (isEnglish) "60 MIN" else "60分前"
                            else -> if (isEnglish) "OFF" else "OFF"
                        }

                        Box(
                            modifier = Modifier
                                .border(1.dp, if (currentMinutes > 0) TextPrimary else BorderSubtle, SpaceXSmallShape)
                                .background(if (currentMinutes > 0) SurfaceElevated else SpaceBlack, shape = SpaceXSmallShape)
                                .clickable {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!hasPermission) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                    val next = when (currentMinutes) {
                                        0 -> 10
                                        10 -> 15
                                        15 -> 30
                                        30 -> 60
                                        else -> 0
                                    }
                                    viewModel.setPhaseEndReminderMinutes(next)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = reminderLabel,
                                color = if (currentMinutes > 0) TextPrimary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. PHASES & SCHEDULE (フェーズ管理とフェーズ時間割を統合)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsSectionHeader("${AppStrings.sectionPhases(lang)} (${state.phases.size})")
                    if (state.phases.size < 8) {
                        SpaceXTextButton(
                            text = if (isEnglish) "+ ADD PHASE" else "+ フェーズ追加",
                            onClick = { showAddPhaseDialog = true }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, SpaceXShape)
                        .background(SurfaceDark, shape = SpaceXShape)
                ) {
                    state.phases.forEachIndexed { index, phase ->
                        val schedule = state.settings.phaseSchedules.find { it.phaseIndex == index }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 左側: フェーズ名（タップで編集）
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { editingPhase = phase },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "0${index + 1} ${phase.name}",
                                            color = TextPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit name",
                                            tint = TextTertiary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Text(
                                        text = AppStrings.itemsCount(lang, phase.items.size),
                                        color = TextTertiary,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // 右側: 開始時刻（タップで編集）+ 削除ボタン
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (schedule != null) {
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, BorderSubtle, SpaceXSmallShape)
                                            .background(SpaceBlack, shape = SpaceXSmallShape)
                                            .clickable { editingSchedule = schedule }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = String.format("%02d:%02d START", schedule.startHour, schedule.startMinute),
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.SansSerif
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit time",
                                                tint = TextTertiary,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }

                                if (state.phases.size > 1) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { viewModel.removePhase(phase.id) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete phase",
                                            tint = TextTertiary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (index < state.phases.size - 1) {
                            SpaceXDivider()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                SpaceXButton(
                    text = if (isEnglish) "RESET TO DEFAULT 4 PHASES" else "4フェーズ構成 (朝/午前/午後/夜) に初期化",
                    onClick = { showResetDefaultConfirm = true },
                    prominent = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. TEMPLATES MANAGEMENT (テンプレート管理: フェーズ数上限)
            item {
                val canAddTemplate = state.templates.size < state.phases.size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsSectionHeader("${AppStrings.sectionTemplates(lang)} (${state.templates.size}/${state.phases.size})")
                    if (canAddTemplate) {
                        SpaceXTextButton(
                            text = if (isEnglish) "+ NEW TEMPLATE" else "+ テンプレート追加",
                            onClick = { onOpenTemplateEditor(null) }
                        )
                    } else {
                        Text(
                            text = if (isEnglish) "MAX REACHED (${state.phases.size})" else "上限に達しました (${state.phases.size})",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                if (state.templates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSubtle, SpaceXShape)
                            .background(SurfaceDark, shape = SpaceXShape)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEnglish) "NO TEMPLATES CONFIGURED" else "登録されたテンプレートはありません",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSubtle, SpaceXShape)
                            .background(SurfaceDark, shape = SpaceXShape)
                    ) {
                        state.templates.forEachIndexed { index, template ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenTemplateEditor(template.id) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = template.name,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = AppStrings.itemsCount(lang, template.items.size),
                                        color = TextTertiary,
                                        fontSize = 10.sp
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.deleteTemplate(template.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = TextTertiary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            if (index < state.templates.size - 1) {
                                SpaceXDivider()
                            }
                        }
                    }
                }
            }

            // 5. HELP, LEGAL & VERSION (チュートリアル・航空思想・規約・バージョン)
            item {
                SettingsSectionHeader(AppStrings.sectionGuide(lang))
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, SpaceXShape)
                        .background(SurfaceDark, shape = SpaceXShape)
                ) {
                    // チュートリアル (文言変更: 「チュートリアル」)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateUserGuide() }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.tutorial(lang),
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open",
                            tint = TextSecondary
                        )
                    }

                    SpaceXDivider()

                    // TIPS: 航空業界の知見・設計思想 (小記事)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAviationTipsDialog = true }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.aviationPhilosophy(lang),
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open",
                            tint = TextSecondary
                        )
                    }

                    SpaceXDivider()

                    // プライバシーポリシー
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigatePrivacyPolicy() }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.privacyPolicy(lang),
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open",
                            tint = TextSecondary
                        )
                    }

                    SpaceXDivider()

                    // バージョン名 & リリースノート (横並びで配置し、各ボタンの高さを統一)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showReleaseNotesDialog = true }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "v0.6.0 (Closed Beta)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = AppStrings.releaseNotes(lang),
                                color = TextSecondary,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Release Notes",
                                tint = TextSecondary
                            )
                        }
                    }

                    SpaceXDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("STORAGE", color = TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
                        Text("LOCAL JSON (OFFLINE ONLY)", color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }

            // 6. DANGER ZONE // データ管理 (全データ初期化ボタン)
            item {
                SettingsSectionHeader(AppStrings.sectionDanger(lang))
                Spacer(modifier = Modifier.height(8.dp))

                SpaceXDangerButton(
                    text = AppStrings.resetAllData(lang),
                    onClick = { showResetAllDataConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Phase Name Edit Dialog
    editingPhase?.let { phase ->
        EditPhaseNameDialog(
            phase = phase,
            onDismiss = { editingPhase = null },
            onConfirm = { newName ->
                viewModel.updatePhaseName(phase.id, newName)
                editingPhase = null
            }
        )
    }

    // Add Phase Dialog
    if (showAddPhaseDialog) {
        AddPhaseDialog(
            onDismiss = { showAddPhaseDialog = false },
            onConfirm = { name ->
                viewModel.addPhase(name)
                showAddPhaseDialog = false
            }
        )
    }

    // Schedule Time Edit Dialog (フェーズ開始時刻の変更)
    editingSchedule?.let { schedule ->
        EditScheduleTimeDialog(
            phaseName = schedule.phaseName,
            initialHour = schedule.startHour,
            initialMinute = schedule.startMinute,
            onDismiss = { editingSchedule = null },
            onConfirm = { newHour, newMinute ->
                viewModel.updatePhaseScheduleTime(schedule.phaseIndex, newHour, newMinute)
                editingSchedule = null
            }
        )
    }

    // Reset 4 Phases Confirmation (不可逆操作防止ダイアログ)
    if (showResetDefaultConfirm) {
        SpaceXConfirmDialog(
            title = if (isEnglish) "CONFIRM RESET: 4 PHASES" else "4フェーズ初期化の確認",
            message = if (isEnglish)
                "Reset all phases and tasks back to the default 4 phases (Morning/Noon/Afternoon/Night)? Current phases will be overwritten."
            else
                "すべてのフェーズとタスクを初期の4フェーズ構成 (朝/午前/午後/夜) に初期化しますか？現在のカスタムフェーズは上書きされます。",
            confirmText = if (isEnglish) "RESET" else "初期化",
            cancelText = AppStrings.cancel(lang),
            onConfirm = {
                viewModel.restoreDefaultFourPhases()
                showResetDefaultConfirm = false
            },
            onDismiss = { showResetDefaultConfirm = false }
        )
    }

    // Reset All Data Confirmation (全データ初期化ダイアログ・赤色ハイライト)
    if (showResetAllDataConfirm) {
        SpaceXConfirmDialog(
            title = AppStrings.resetAllData(lang),
            message = AppStrings.resetAllDataConfirm(lang),
            confirmText = if (isEnglish) "RESET ALL" else "全初期化",
            cancelText = AppStrings.cancel(lang),
            isDanger = true,
            onConfirm = {
                viewModel.resetAllData()
                showResetAllDataConfirm = false
            },
            onDismiss = { showResetAllDataConfirm = false }
        )
    }

    // Release Notes Dialog
    if (showReleaseNotesDialog) {
        ReleaseNotesDialog(
            isEnglish = isEnglish,
            onDismiss = { showReleaseNotesDialog = false }
        )
    }

    // Aviation Tips Philosophy Dialog
    if (showAviationTipsDialog) {
        AviationTipsDialog(
            isEnglish = isEnglish,
            onDismiss = { showAviationTipsDialog = false }
        )
    }
}

// SCHEDULE TIME EDIT DIALOG (SpaceX ミニマル時間設定)
@Composable
fun EditScheduleTimeDialog(
    phaseName: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceBlack, shape = SpaceXShape)
                .border(1.dp, BorderProminent, SpaceXShape)
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "SCHEDULE TIME // $phaseName".uppercase(),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "フェーズ開始時刻を設定",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour picker / display
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { hour = (hour + 1) % 24 }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Hour Up", tint = TextPrimary)
                        }
                        Text(
                            text = String.format("%02d", hour),
                            color = TextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        IconButton(onClick = { hour = (hour + 23) % 24 }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Hour Down", tint = TextPrimary)
                        }
                    }

                    Text(
                        text = ":",
                        color = TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Minute picker / display
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { minute = (minute + 5) % 60 }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Minute Up", tint = TextPrimary)
                        }
                        Text(
                            text = String.format("%02d", minute),
                            color = TextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        IconButton(onClick = { minute = (minute + 55) % 60 }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minute Down", tint = TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpaceXTextButton(text = "CANCEL", onClick = onDismiss)
                    Spacer(modifier = Modifier.width(12.dp))
                    SpaceXButton(
                        text = "SAVE",
                        onClick = { onConfirm(hour, minute) },
                        prominent = true
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionButton(
    text: String,
    subtext: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) com.example.checklist.theme.SpaceXAccent else BorderSubtle,
                shape = SpaceXShape
            )
            .background(if (isSelected) SurfaceElevated else SurfaceDark, shape = SpaceXShape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                color = if (isSelected) com.example.checklist.theme.SpaceXAccent else TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                color = TextTertiary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Box(
            modifier = Modifier
                .border(1.dp, if (enabled) com.example.checklist.theme.SpaceXAccent else BorderSubtle, SpaceXSmallShape)
                .background(if (enabled) SurfaceElevated else SpaceBlack, shape = SpaceXSmallShape)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (enabled) "ON" else "OFF",
                color = if (enabled) TextPrimary else TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun EditPhaseNameDialog(
    phase: Phase,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var name by remember { mutableStateOf(phase.name) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceBlack, shape = SpaceXShape)
                .border(1.dp, BorderProminent, SpaceXShape)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "EDIT PHASE NAME",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .border(1.dp, BorderSubtle, SpaceXShape)
                        .background(SurfaceDark, shape = SpaceXShape)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif
                        ),
                        cursorBrush = SolidColor(TextPrimary),
                        modifier = Modifier.fillMaxSize(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                            if (name.trim().isNotEmpty()) onConfirm(name.trim())
                        })
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    SpaceXTextButton(text = "CANCEL", onClick = onDismiss)
                    Spacer(modifier = Modifier.width(8.dp))
                    SpaceXButton(
                        text = "SAVE",
                        onClick = {
                            if (name.trim().isNotEmpty()) onConfirm(name.trim())
                        },
                        enabled = name.trim().isNotEmpty()
                    )
                }
            }
        }
    }
}

@Composable
private fun AddPhaseDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val isNameValid = name.trim().isNotEmpty()

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceBlack, shape = SpaceXShape)
                .border(1.dp, BorderProminent, SpaceXShape)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "ADD FLIGHT PHASE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .border(1.dp, BorderSubtle, SpaceXShape)
                        .background(SurfaceDark, shape = SpaceXShape)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif
                        ),
                        cursorBrush = SolidColor(TextPrimary),
                        modifier = Modifier.fillMaxSize(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                            if (isNameValid) onConfirm(name.trim())
                        })
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    SpaceXTextButton(text = "CANCEL", onClick = onDismiss)
                    Spacer(modifier = Modifier.width(8.dp))
                    SpaceXButton(
                        text = "ADD",
                        onClick = {
                            if (isNameValid) onConfirm(name.trim())
                        },
                        enabled = isNameValid
                    )
                }
            }
        }
    }
}

// RELEASE NOTES DIALOG (v0.5.0, v0.4.1, etc.)
@Composable
fun ReleaseNotesDialog(
    isEnglish: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceBlack, shape = SpaceXShape)
                .border(1.dp, BorderProminent, SpaceXShape)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isEnglish) "RELEASE NOTES" else "リリースノート",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                // v0.6.0
                Text(
                    text = "v0.6.0 (Closed Beta)",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                val notesV060 = if (isEnglish) {
                    """
                    • Default Language: Set default language to Japanese.
                    • Priority Terms Refined: Replaced REQ/NORM with Standard/Critical (重要/標準).
                    • Accidental Tap Prevention: Tasks no longer complete by tapping card body; only dedicated checkbox or dismiss icon triggers action.
                    • Visual Glow Accents: Tron and Monster themes now highlight outer borders in vivid Cyber Red and Energy Green on pure black background.
                    • Smart Action Button: Advance Phase button remains tucked in overflow menu until all critical items are cleared.
                    • De-cluttered List: Removed 'Active Tasks' label and count badges for finished sections.
                    • Smooth Transitions: Added light 180ms crossfade between phases and stabilized drag-and-drop animation.
                    • Calendar Visibility: Added subtle blue to Saturday and red to Sunday, fixing Monday column display.
                    • Layout Balancing: Aligned clear button with pending text and grouped theme buttons cleanly.
                    • SpaceX Cockpit Philosophy: Added insights on Crew Dragon HUD minimalism to guide philosophy.
                    """.trimIndent()
                } else {
                    """
                    ・デフォルト言語を日本語に変更: 初期起動時の言語を日本語に設定。
                    ・タスク優先度の表記刷新: NORM/REQを「標準」「重要」（英語はSTD/CRIT）に変更。
                    ・誤タッチ防止機構: カード全体タップでの「済」を廃止。チェックボックスか✕ボタンのみで操作。
                    ・外縁アクセント発光: トロン（赤）とモンスター（緑）で外縁枠線のみを鮮やかに発光させ、他は純黒・白文字に統一。
                    ・フェーズ完了ボタンのスマート制御: 重要タスク未完了時は下部ボタンを隠し、右上の詳細メニューに格納。全重要タスク完了時に下部に出現。
                    ・視覚ノイズ削減: 「進行中タスク」ヘッダーの削除および完了セクションの件数表記を削除。
                    ・上品なフェーズ遷移と並び替え改善: フェーズ切り替え時の軽量クロスフェード、ドラッグ並び替え時の追従アニメーション競合を解消。
                    ・カレンダー視認性向上: 土曜に青、日曜に赤を控えめに追加し、月曜列の表示薄れを修正。
                    ・レイアウト均等化: 完了消去ボタンと保留件数の縦位置統一、テーマ選択ボタンの配置整理。
                    ・SpaceX設計思想の追記: クルードラゴンのタッチHUD思想と極限の自動化についての小記事を増補。
                    """.trimIndent()
                }
                Text(
                    text = notesV060,
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                SpaceXDivider()
                Spacer(modifier = Modifier.height(14.dp))

                // v0.5.0
                Text(
                    text = "v0.5.0 (Closed Beta)",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val notesV050 = if (isEnglish) {
                    "• REQ visual borders, Tron/Monster themes, bilingual language switch, phase end reminders, notification tap resume."
                } else {
                    "・REQ枠線の視覚的強調、トロン/モンスターテーマ、日英切り替え、フェーズ終了前リマインダー、通知タップ復帰。"
                }
                Text(
                    text = notesV050,
                    color = TextTertiary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    SpaceXButton(
                        text = if (isEnglish) "CLOSE" else "閉じる",
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

// AVIATION & SPACEX PHILOSOPHY DIALOG (航空業界・スペースXの知見小記事)
@Composable
fun AviationTipsDialog(
    isEnglish: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceBlack, shape = SpaceXShape)
                .border(1.dp, BorderProminent, SpaceXShape)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isEnglish) "AVIATION & SPACEX PHILOSOPHY" else "航空チェックリストとSpaceXの設計思想",
                    color = TextPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                val content = if (isEnglish) {
                    """
                    1. Operational Phases (フェーズ運用)
                    Aircraft operations strictly separate checklists into distinct flight phases: Pre-flight, Takeoff, Cruise, Descent, and Landing. This app segments daily human routines into phases (Morning, Noon, Afternoon, Night) so your cognitive capacity is 100% focused on immediate objectives without distraction.

                    2. Discipline of Critical vs Standard (重要と標準の厳格な規律)
                    Aviation distinguishes between 'Critical / Emergency' procedures and 'Normal' procedures. Critical tasks represent vital non-negotiable checklist items. They stand out with prominent borders and are automatically carried forward until executed.

                    3. Visual De-cluttering (視覚ノイズの除去)
                    Completed tasks fade out into borderless rows, leaving only pending critical tasks prominent. Just like modern glass cockpit HUDs, unnecessary visual noise is eliminated to maintain situational awareness.

                    4. Guarded Actions & Challenge-Response (確実な実行と誤操作防止)
                    Each item requires a deliberate checkbox tap to verify completion, rather than tapping anywhere on the card. Important destructive actions are tucked into menus until prerequisites are met, preventing inadvertent mistakes.

                    5. SpaceX Crew Dragon UI Philosophy (スペースXの極限ミニマリズム)
                    SpaceX's Crew Dragon spacecraft replaced thousands of physical cockpit switches with streamlined touch screens, showing astronauts only the information critical to the current phase of flight. By automating routines and surfacing only high-priority tasks, situational awareness reaches its highest efficiency.
                    """.trimIndent()
                } else {
                    """
                    1. フェーズ運用（Operational Phases）
                    航空機は「離陸前」「離陸」「巡航」「降下」「着陸」と飛行段階ごとにチェックリストを厳格に切り替えます。着陸態勢に入ったパイロットが離陸のタスクに気を取られることはありません。本アプリも1日をフェーズ（朝・午前・午後・夜）に区切ることで、認知負荷を最小化し、今この瞬間のミッションに100%集中させる設計です。

                    2. 重要（CRIT）と標準（STD）の規律
                    航空運航における「クリティカルチェックリスト」と「ノーマルチェックリスト」の厳密な峻別です。生命線となる重要タスクはアクセント枠で力強く存在感を示し、完了するまで次フェーズ・翌日に引き継がれます（自動キャリーオーバー）。

                    3. デクラッター（Visual De-cluttering: 視覚ノイズの除去）
                    完了したタスクの枠線を消し、未完了のタスクのみを高コントラストで際立たせます。最新鋭のグラスコクピットHUDと同様に、終わった不要な情報を削ぎ落とすことで状況認識（Situational Awareness）を最高度に維持します。

                    4. Guarded Action（誤操作防止と確実な実行）
                    カード全体のどこを触っても完了してしまう仕様を排除し、指差呼称のように目的のチェックボックスを確実にタップした時のみ完了状態へ移行します。また重要タスクが終わるまでフェーズ完了ボタンを前面に出さない構造により、誤操作の芽を摘み取ります。

                    5. SpaceX クルードラゴンのコクピットUI哲学
                    SpaceXの有人宇宙船クルードラゴン（Crew Dragon）は、従来の宇宙船にあった数千個の物理スイッチを撤廃し、大画面タッチHUD主体の極限のミニマリズムへ刷新されました。飛行フェーズに応じた必要最小限のUIのみを提示し、定常業務を自動化することで宇宙飛行士の認知的負担を劇的に削減しています。本アプリもこの哲学を受け継ぎ、不要な装飾を排した高コントラストな設計を徹底しています。
                    """.trimIndent()
                }

                Text(
                    text = content,
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    SpaceXButton(
                        text = if (isEnglish) "CLOSE" else "閉じる",
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

