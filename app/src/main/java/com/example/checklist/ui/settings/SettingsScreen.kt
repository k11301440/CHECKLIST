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
import com.example.checklist.data.AppState
import com.example.checklist.data.Phase
import com.example.checklist.data.PhaseTemplate
import com.example.checklist.data.ThemeMode
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
import com.example.checklist.ui.components.SpaceXDivider
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
                text = "SYSTEM SETTINGS",
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
            // 1. THEME MODE (白黒テーマ切り替え)
            item {
                SettingsSectionHeader("THEME // テーマ")
                Spacer(modifier = Modifier.height(8.dp))

                val currentMode = state.settings.themeMode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOptionButton(
                        text = "SPACE BLACK",
                        subtext = "ダーク",
                        isSelected = currentMode == ThemeMode.SPACE_BLACK,
                        onClick = { viewModel.setThemeMode(ThemeMode.SPACE_BLACK) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionButton(
                        text = "SPACE WHITE",
                        subtext = "ライト",
                        isSelected = currentMode == ThemeMode.SPACE_WHITE,
                        onClick = { viewModel.setThemeMode(ThemeMode.SPACE_WHITE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. MISSION AUTOMATION (時間自動進行 & REQ/NORMキャリーオーバー & 完了確認)
            item {
                SettingsSectionHeader("AUTOMATION // 運用自動化")
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                        .background(SurfaceDark)
                ) {
                    // 自動進行トグル
                    SettingToggleRow(
                        title = "時間による自動進行",
                        description = "時刻（朝/午前/午後/夜）に応じて現在フェーズを自動切り替え",
                        enabled = state.settings.autoAdvanceByTime,
                        onToggle = { viewModel.setAutoAdvanceByTime(!state.settings.autoAdvanceByTime) }
                    )

                    SpaceXDivider()

                    // 未完了REQタスク持ち越しトグル
                    SettingToggleRow(
                        title = "未完了REQの自動引き継ぎ",
                        description = "前のセクションで終わらなかった必須タスクを次フェーズ・翌日に持ち越し",
                        enabled = state.settings.carryoverUnfinishedReq,
                        onToggle = { viewModel.setCarryoverUnfinishedReq(!state.settings.carryoverUnfinishedReq) }
                    )

                    SpaceXDivider()

                    // 未完了NORMタスク持ち越しトグル (フィードバック対応)
                    SettingToggleRow(
                        title = "未完了NORMの自動引き継ぎ",
                        description = "前のセクションで終わらなかった普通タスクも次フェーズに持ち越し",
                        enabled = state.settings.carryoverUnfinishedNorm,
                        onToggle = { viewModel.setCarryoverUnfinishedNorm(!state.settings.carryoverUnfinishedNorm) }
                    )

                    SpaceXDivider()

                    // フェーズ完了時の確認トグル
                    SettingToggleRow(
                        title = "フェーズ進行時の確認",
                        description = "次のフェーズへ進む際に完了確認ダイアログを表示する",
                        enabled = state.settings.confirmReviewOnAdvance,
                        onToggle = { viewModel.setConfirmReviewOnAdvance(!state.settings.confirmReviewOnAdvance) }
                    )

                    SpaceXDivider()

                    // 未完了REQタスク通知トグル (オプトイン)
                    SettingToggleRow(
                        title = "未完了REQタスクの通知",
                        description = "現在のフェーズで未処理の必須タスクをAndroid通知バーに常時表示",
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
                }
            }

            // 3. PHASE TIME WINDOWS (フェーズ時間割: タップで開始時刻を編集可能)
            item {
                SettingsSectionHeader("SCHEDULE // フェーズ時間割 (タップで編集)")
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                        .background(SurfaceDark)
                ) {
                    state.settings.phaseSchedules.forEachIndexed { sIdx, schedule ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingSchedule = schedule }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PHASE 0${schedule.phaseIndex + 1} ${schedule.phaseName}",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = String.format("%02d:%02d START", schedule.startHour, schedule.startMinute),
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Time",
                                    tint = TextTertiary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        if (sIdx < state.settings.phaseSchedules.size - 1) {
                            SpaceXDivider()
                        }
                    }
                }
            }

            // 4. PHASES CONFIGURATION (フェーズ管理)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsSectionHeader("PHASES // フェーズ管理 (${state.phases.size})")
                    if (state.phases.size < 8) {
                        SpaceXTextButton(
                            text = "+ ADD PHASE",
                            onClick = { showAddPhaseDialog = true }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                        .background(SurfaceDark)
                ) {
                    state.phases.forEachIndexed { index, phase ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "0${index + 1} ${phase.name}",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${phase.items.size} ITEMS",
                                    color = TextTertiary,
                                    fontSize = 10.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { editingPhase = phase },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit name",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                if (state.phases.size > 1) {
                                    IconButton(
                                        onClick = { viewModel.removePhase(phase.id) },
                                        modifier = Modifier.size(32.dp)
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
                    text = "4フェーズ構成 (朝/午前/午後/夜) に初期化",
                    onClick = { showResetDefaultConfirm = true },
                    prominent = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 5. TEMPLATES MANAGEMENT (テンプレート管理: フェーズ数上限)
            item {
                val canAddTemplate = state.templates.size < state.phases.size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsSectionHeader("TEMPLATES (${state.templates.size}/${state.phases.size})")
                    if (canAddTemplate) {
                        SpaceXTextButton(
                            text = "+ NEW TEMPLATE",
                            onClick = { onOpenTemplateEditor(null) }
                        )
                    } else {
                        Text(
                            text = "MAX REACHED // 上限(${state.phases.size})",
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
                            .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO TEMPLATES CONFIGURED",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                            .background(SurfaceDark)
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
                                        text = "${template.items.size} ITEMS",
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

            // 6. STORE & PRIVACY COMPLIANCE (ストア要件・チュートリアル・ポリシー)
            item {
                SettingsSectionHeader("HELP & LEGAL // ガイドと規約")
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                        .background(SurfaceDark)
                ) {
                    // チュートリアル・機能説明
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateUserGuide() }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "使い方ガイド // チュートリアル",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "4フェーズ運用、タスク並び替え、振り返り機能の解説",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
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
                            text = "プライバシーポリシー",
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("APPLICATION VERSION", color = TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
                        Text("v0.3.0 (Closed Beta)", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.SansSerif)
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
        com.example.checklist.ui.components.SpaceXConfirmDialog(
            title = "CONFIRM RESET // 4 PHASES",
            message = "すべてのフェーズとタスクを初期の4フェーズ構成 (朝/午前/午後/夜) に初期化しますか？現在のカスタムフェーズは上書きされます。",
            confirmText = "RESET",
            onConfirm = {
                viewModel.restoreDefaultFourPhases()
                showResetDefaultConfirm = false
            },
            onDismiss = { showResetDefaultConfirm = false }
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
                .background(SpaceBlack)
                .border(1.dp, BorderProminent, RoundedCornerShape(2.dp))
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
                color = if (isSelected) TextPrimary else BorderSubtle,
                shape = RoundedCornerShape(2.dp)
            )
            .background(if (isSelected) SurfaceElevated else SurfaceDark)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                color = if (isSelected) TextPrimary else TextSecondary,
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
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
        Box(
            modifier = Modifier
                .border(1.dp, if (enabled) TextPrimary else BorderSubtle, RoundedCornerShape(2.dp))
                .background(if (enabled) SurfaceElevated else SpaceBlack)
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
                .background(SpaceBlack)
                .border(1.dp, BorderProminent, RoundedCornerShape(2.dp))
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
                        .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                        .background(SurfaceDark)
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
                        singleLine = true
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
                        onClick = { onConfirm(name) },
                        enabled = name.isNotBlank()
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

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceBlack)
                .border(1.dp, BorderProminent, RoundedCornerShape(2.dp))
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
                        .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                        .background(SurfaceDark)
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
                        singleLine = true
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
                        onClick = { onConfirm(name) },
                        enabled = name.isNotBlank()
                    )
                }
            }
        }
    }
}
