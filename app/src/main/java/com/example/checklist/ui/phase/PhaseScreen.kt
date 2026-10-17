package com.example.checklist.ui.phase

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.checklist.data.AppLanguage
import com.example.checklist.data.AppState
import com.example.checklist.data.ChecklistItem
import com.example.checklist.data.Phase
import com.example.checklist.data.Priority
import com.example.checklist.ui.common.AppStrings
import com.example.checklist.theme.BorderProminent
import com.example.checklist.theme.BorderSubtle
import com.example.checklist.theme.SpaceBlack
import com.example.checklist.theme.SpaceXLineBreak
import com.example.checklist.theme.SurfaceDark
import com.example.checklist.theme.SurfaceElevated
import com.example.checklist.theme.TextDisabled
import com.example.checklist.theme.TextPrimary
import com.example.checklist.theme.TextSecondary
import com.example.checklist.theme.TextTertiary
import com.example.checklist.theme.formatTaskText
import com.example.checklist.ui.ChecklistViewModel
import com.example.checklist.ui.components.SpaceXButton
import com.example.checklist.ui.components.SpaceXCheckbox
import com.example.checklist.ui.components.SpaceXConfirmDialog
import com.example.checklist.ui.components.SpaceXDivider
import com.example.checklist.ui.components.SpaceXShape
import com.example.checklist.ui.components.SpaceXSmallShape
import com.example.checklist.ui.components.SpaceXTextButton
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhaseScreen(
    state: AppState,
    viewModel: ChecklistViewModel,
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateCalendar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (state.phases.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().background(SpaceBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NO ACTIVE PHASES",
                    color = TextSecondary,
                    letterSpacing = 2.sp,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                SpaceXButton(
                    text = "RESTORE 4 PHASES (朝・午前・午後・夜)",
                    onClick = { viewModel.restoreDefaultFourPhases() }
                )
            }
        }
        return
    }

    val isEnglish = state.settings.appLanguage == AppLanguage.EN
    val selectedIndex = state.selectedPhaseIndex.coerceIn(0, state.phases.size - 1)
    val currentPhase = state.phases[selectedIndex]

    val activeItems = currentPhase.items.filter { !it.isCompleted && !it.isDeleted }
    val activeRequired = activeItems.filter { it.priority == Priority.REQUIRED }
    val activeNormal = activeItems.filter { it.priority == Priority.NORMAL }
    val completedItems = currentPhase.items.filter { it.isCompleted && !it.isDeleted }
    val discardedItems = currentPhase.items.filter { it.isDeleted }
    val totalRequired = currentPhase.items.filter { it.priority == Priority.REQUIRED && !it.isDeleted }

    val remainingRequiredCount = activeRequired.size
    val completedTotalCount = completedItems.size
    val allRequiredCompleted = activeRequired.isEmpty() && totalRequired.isNotEmpty()
    val allTasksCompleted = activeRequired.isEmpty() && activeNormal.isEmpty() && (completedItems.isNotEmpty())

    var isNormalExpanded by remember { mutableStateOf(true) }
    var isCompletedExpanded by remember { mutableStateOf(true) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var showResetDefaultDialog by remember { mutableStateOf(false) }
    var showResetPhaseDialog by remember { mutableStateOf(false) }
    var showClearCompletedDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ChecklistItem?>(null) }
    var showTemplateSelectDialog by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // ドラッグ＆ドロップ並び替え用ステート
    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val itemHeightPx = with(LocalDensity.current) { 60.dp.toPx() }
    val currentAppState by rememberUpdatedState(state)

    // 現在時刻に基づくフェーズの判定（タスク作成時のデフォルトフェーズ用）
    val currentPhaseIndexByTime = remember(state.phases, state.settings.phaseSchedules) {
        val now = LocalTime.now()
        val currentMinutes = now.hour * 60 + now.minute
        val schedules = state.settings.phaseSchedules
        if (schedules.isNotEmpty() && schedules.size == state.phases.size) {
            val sorted = schedules.sortedBy { it.startHour * 60 + it.startMinute }
            var targetIdx = sorted.last().phaseIndex
            for (i in sorted.indices.reversed()) {
                val sMinutes = sorted[i].startHour * 60 + sorted[i].startMinute
                if (currentMinutes >= sMinutes) {
                    targetIdx = sorted[i].phaseIndex
                    break
                }
            }
            targetIdx.coerceIn(0, state.phases.size - 1)
        } else {
            when {
                currentMinutes < 9 * 60 -> 0
                currentMinutes < 13 * 60 -> 1
                currentMinutes < 18 * 60 -> 2
                else -> 3
            }.coerceIn(0, state.phases.size - 1)
        }
    }

    // 常時入力用のステート（ダイアログなしでいつでも直接入力可能）
    var quickInputTitle by remember { mutableStateOf("") }
    var quickInputPriority by remember { mutableStateOf(Priority.NORMAL) }
    var quickTargetPhaseIndex by remember { mutableStateOf(currentPhaseIndexByTime) }
    var quickTargetDayOffset by remember { mutableStateOf(0) } // 0: 今日, 1: 明日, 2: 明後日
    var showQuickOptions by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // ROW 1: TOP NAVIGATION (Calendar / Home / Settings / Menu)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateCalendar,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Flight Calendar",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onNavigateHome,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home Overview",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onNavigateSettings,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(SurfaceDark, shape = SpaceXShape)
                            .border(1.dp, BorderProminent, SpaceXShape)
                    ) {
                        // フェーズ完了して次へ（普段はメニューに格納）
                        val isLast = selectedIndex >= state.phases.size - 1
                        val advanceLabel = if (isLast) {
                            if (isEnglish) "Complete Day & Restart" else "1日のタスクをすべて完了"
                        } else {
                            if (isEnglish) "Complete Phase & Advance" else "フェーズ完了して次へ"
                        }
                        DropdownMenuItem(
                            text = { Text(advanceLabel, color = TextPrimary, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                if (state.settings.confirmReviewOnAdvance) {
                                    showReviewDialog = true
                                } else {
                                    viewModel.completePhase(currentPhase.id, false, null)
                                }
                            }
                        )
                        if (completedTotalCount > 0) {
                            DropdownMenuItem(
                                text = { Text("完了したタスクを整理 (削除)", color = TextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    showMenu = false
                                    showClearCompletedDialog = true
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (isEnglish) "Apply Template..." else "テンプレートを適用...", color = TextPrimary, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                showTemplateSelectDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isEnglish) "Save Current as Template" else "現在のリストをテンプレート保存", color = TextPrimary, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                viewModel.savePhaseAsTemplate(currentPhase.id, "${currentPhase.name} テンプレート")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isEnglish) "Reset This Phase" else "このフェーズをリセット", color = TextPrimary, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                showResetPhaseDialog = true
                            }
                        )
                    }
                }
            }
        }

        // ROW 2: 4-PHASE OVERVIEW BAR (画面を横に4等分して朝〜夜の各フェーズが一望できる構成)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            state.phases.forEachIndexed { index, phase ->
                val isSelected = index == selectedIndex
                val phaseNumber = String.format("%02d", index + 1)
                val label = "$phaseNumber ${phase.name}"

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) com.example.checklist.theme.SpaceXAccent else BorderSubtle,
                            shape = SpaceXShape
                        )
                        .background(if (isSelected) SurfaceElevated else SpaceBlack, shape = SpaceXShape)
                        .clickable { viewModel.selectPhase(index) }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // SEEKBAR-STYLE PROGRESS LINE (現在フェーズまで太く、左から右へシークバーのように表示)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // 背景のベースライン (細線 1.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BorderSubtle)
            )
            // 現在選択・進行フェーズまでのプログレス太線 (太線 3.dp: アクセント色連動)
            val progressFraction = ((selectedIndex + 1).toFloat() / state.phases.size.coerceAtLeast(1)).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progressFraction)
                    .height(3.dp)
                    .background(com.example.checklist.theme.SpaceXAccent, shape = RoundedCornerShape(1.5.dp))
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        val todayStr = remember(state.currentDate) {
            runCatching {
                LocalDate.parse(state.currentDate)
                    .format(DateTimeFormatter.ofPattern("yyyy.MM.dd EEE", Locale.ENGLISH))
                    .uppercase()
            }.getOrElse {
                LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("yyyy.MM.dd EEE", Locale.ENGLISH))
                    .uppercase()
            }
        }

        // PHASE STATUS HEADER
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "$todayStr // PHASE 0${selectedIndex + 1}",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = currentPhase.name.uppercase(),
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                lineHeight = 36.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            // STATUS TEXT & CLEAR DONE BUTTON (完全に同じ横一列で垂直中央揃え)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusText = if (remainingRequiredCount > 0) {
                    if (isEnglish) "$remainingRequiredCount CRITICAL PENDING" else "重要タスク残り $remainingRequiredCount 件"
                } else if (totalRequired.isNotEmpty()) {
                    if (isEnglish) "ALL CRITICAL CHECKED" else "重要タスク全完了"
                } else {
                    if (isEnglish) "NO CRITICAL ITEMS" else "重要タスクなし"
                }
                Text(
                    text = statusText,
                    color = if (remainingRequiredCount > 0) com.example.checklist.theme.SpaceXAccent else TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp
                )

                // 完了済み単発タスクのクリアボタン（誤破棄防止の確認ダイアログ付き）
                if (completedTotalCount > 0) {
                    SpaceXTextButton(
                        text = if (isEnglish) "CLEAR DONE ($completedTotalCount)" else "完了消去 ($completedTotalCount)",
                        onClick = { showClearCompletedDialog = true },
                        prominent = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CHECKLIST ITEMS (フェーズ遷移時は軽やかなクロスフェードアニメーション)
        Crossfade(
            targetState = selectedIndex,
            animationSpec = androidx.compose.animation.core.tween(180),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            label = "PhaseCrossfade"
        ) { targetIdx ->
            val phaseForList = state.phases.getOrNull(targetIdx) ?: currentPhase
            val activeForList = phaseForList.items.filter { !it.isCompleted && !it.isDeleted }
            val completedForList = phaseForList.items.filter { it.isCompleted && !it.isDeleted }
            val discardedForList = phaseForList.items.filter { it.isDeleted }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 初回起動時などタスクが空の時の控えめな案内
                if (activeForList.isEmpty() && completedForList.isEmpty() && discardedForList.isEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(28.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderSubtle, SpaceXShape)
                                .background(SurfaceDark.copy(alpha = 0.5f), shape = SpaceXShape)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "NO TASKS REGISTERED",
                                    color = TextTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "このフェーズにはタスクがありません",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "下部の入力バーから新しいタスクを追加するか、\n右上の設定 > テンプレートからプリセットを読み込んでください。",
                                    color = TextTertiary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // ACTIVE TASKS (REQ / NORM を単一リストとして描画し、自由に相互並び替え可能)
                if (activeForList.isNotEmpty()) {
                    itemsIndexed(activeForList, key = { _, item -> item.id }) { activeIndex, item ->
                        val isDragging = item.id == draggingItemId
                        ChecklistItemRow(
                            item = item,
                            isCritical = item.priority == Priority.REQUIRED,
                            isDragging = isDragging,
                            dragOffsetY = dragOffsetY,
                            modifier = if (isDragging) Modifier else Modifier.animateItem(),
                            onTap = { viewModel.completeItem(phaseForList.id, item.id) },
                            onLongPress = { viewModel.undoItem(phaseForList.id, item.id) },
                            onEdit = { editingItem = item },
                            onDelete = { viewModel.removeItem(phaseForList.id, item.id) },
                            onDragStart = {
                                draggingItemId = item.id
                                dragOffsetY = 0f
                            },
                            onDragDelta = { deltaY ->
                                dragOffsetY += deltaY
                                val latestPhase = currentAppState.phases.getOrNull(targetIdx) ?: phaseForList
                                val latestActive = latestPhase.items.filter { !it.isCompleted && !it.isDeleted }
                                val currentIndex = latestActive.indexOfFirst { it.id == item.id }
                                val threshold = itemHeightPx * 0.6f
                                if (currentIndex != -1) {
                                    if (dragOffsetY > threshold && currentIndex < latestActive.size - 1) {
                                        val nextItem = latestActive[currentIndex + 1]
                                        val fromIdx = latestPhase.items.indexOfFirst { it.id == item.id }
                                        val toIdx = latestPhase.items.indexOfFirst { it.id == nextItem.id }
                                        if (fromIdx != -1 && toIdx != -1) {
                                            viewModel.reorderItems(latestPhase.id, fromIdx, toIdx)
                                            dragOffsetY -= itemHeightPx
                                        }
                                    } else if (dragOffsetY < -threshold && currentIndex > 0) {
                                        val prevItem = latestActive[currentIndex - 1]
                                        val fromIdx = latestPhase.items.indexOfFirst { it.id == item.id }
                                        val toIdx = latestPhase.items.indexOfFirst { it.id == prevItem.id }
                                        if (fromIdx != -1 && toIdx != -1) {
                                            viewModel.reorderItems(latestPhase.id, fromIdx, toIdx)
                                            dragOffsetY += itemHeightPx
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingItemId = null
                                dragOffsetY = 0f
                            }
                        )
                    }
                }

                // COMPLETED & ARCHIVED SECTION (完了したタスクと保管された削除タスクを下に移動)
                if (completedForList.isNotEmpty() || discardedForList.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(onClick = { isCompletedExpanded = !isCompletedExpanded })
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = AppStrings.completedAndDiscarded(state.settings.appLanguage),
                                color = TextTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Icon(
                                imageVector = if (isCompletedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isCompletedExpanded) "Collapse" else "Expand",
                                tint = TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (isCompletedExpanded) {
                        // 完了タスク
                        items(completedForList, key = { it.id }) { item ->
                            ChecklistItemRow(
                                item = item,
                                isCritical = item.priority == Priority.REQUIRED,
                                onTap = { /* 完了済み */ },
                                onLongPress = { viewModel.undoItem(phaseForList.id, item.id) },
                                onEdit = { editingItem = item },
                                onDelete = { viewModel.removeItem(phaseForList.id, item.id) }
                            )
                        }

                        // 破棄タスク（誤破棄防止のため保管・ワンタップ復元可能）
                        items(discardedForList, key = { it.id }) { item ->
                            DiscardedItemRow(
                                item = item,
                                onRestore = { viewModel.restoreItem(phaseForList.id, item.id) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // ALWAYS-AVAILABLE QUICK TASK BAR (仕事用の単発タスクをいつでも即追加)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp)
        ) {
            val submitQuickTask = {
                if (quickInputTitle.isNotBlank()) {
                    val targetPhase = state.phases.getOrNull(quickTargetPhaseIndex) ?: currentPhase
                    val targetDate = if (quickTargetDayOffset > 0) {
                        val base = runCatching { LocalDate.parse(state.currentDate) }.getOrElse { LocalDate.now() }
                        base.plusDays(quickTargetDayOffset.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
                    } else null
                    viewModel.addItem(targetPhase.id, quickInputTitle, quickInputPriority, scheduledDate = targetDate)
                    quickInputTitle = ""
                    showQuickOptions = false
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, SpaceXShape)
                    .background(SurfaceDark, shape = SpaceXShape)
                    .padding(start = 4.dp, top = 4.dp, bottom = 4.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority switch (重要 / 標準 または CRIT / STD)
                val priorityLabel = if (quickInputPriority == Priority.REQUIRED) {
                    AppStrings.priorityCritical(state.settings.appLanguage)
                } else {
                    AppStrings.priorityStandard(state.settings.appLanguage)
                }
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .border(
                            width = 1.dp,
                            color = if (quickInputPriority == Priority.REQUIRED) com.example.checklist.theme.SpaceXAccent else BorderSubtle,
                            shape = SpaceXSmallShape
                        )
                        .background(if (quickInputPriority == Priority.REQUIRED) SurfaceElevated else SpaceBlack, shape = SpaceXSmallShape)
                        .combinedClickable(onClick = {
                            quickInputPriority = if (quickInputPriority == Priority.REQUIRED) Priority.NORMAL else Priority.REQUIRED
                        })
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = priorityLabel,
                        color = if (quickInputPriority == Priority.REQUIRED) com.example.checklist.theme.SpaceXAccent else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Text field
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = quickInputTitle,
                        onValueChange = {
                            quickInputTitle = it
                            if (it.isNotEmpty()) showQuickOptions = true
                        },
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif
                        ),
                        cursorBrush = SolidColor(TextPrimary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitQuickTask() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (quickInputTitle.isEmpty()) {
                        Text(
                            text = if (isEnglish) "Enter new task..." else "新しいタスクを入力...",
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Options toggle (いつのタスクか選択を展開)
                IconButton(
                    onClick = { showQuickOptions = !showQuickOptions },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (showQuickOptions) Icons.Default.ExpandLess else Icons.Default.DateRange,
                        contentDescription = "Schedule",
                        tint = if (quickTargetDayOffset > 0) TextPrimary else TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Add button: 紙飛行機アイコン (Send)
                IconButton(
                    onClick = { submitQuickTask() },
                    modifier = Modifier.size(32.dp),
                    enabled = quickInputTitle.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Add Task",
                        tint = if (quickInputTitle.isNotBlank()) TextPrimary else TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // いつ・どのフェーズのタスクか選べるオプション行（明日以降に始まるタスクも設定可能）
            if (showQuickOptions) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, SpaceXShape)
                        .background(SurfaceDark, shape = SpaceXShape)
                        .padding(8.dp)
                ) {
                    Text(
                        text = AppStrings.scheduleSelect(state.settings.appLanguage),
                        color = TextTertiary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // 日付選択 (設定言語に応じて切替)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val dayLabels = if (isEnglish) {
                            listOf("TODAY" to 0, "TOMORROW" to 1, "IN 2 DAYS" to 2)
                        } else {
                            listOf("今日" to 0, "明日" to 1, "明後日" to 2)
                        }
                        for ((label, offset) in dayLabels) {
                            val isSelected = quickTargetDayOffset == offset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, if (isSelected) TextPrimary else BorderSubtle, SpaceXSmallShape)
                                    .background(if (isSelected) SurfaceElevated else SpaceBlack, shape = SpaceXSmallShape)
                                    .clickable { quickTargetDayOffset = offset }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    // フェーズ選択
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.phases.forEachIndexed { pIdx, p ->
                            val isSelected = quickTargetPhaseIndex == pIdx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, if (isSelected) TextPrimary else BorderSubtle, SpaceXSmallShape)
                                    .background(if (isSelected) SurfaceElevated else SpaceBlack, shape = SpaceXSmallShape)
                                    .clickable { quickTargetPhaseIndex = pIdx }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = p.name,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // BOTTOM ACTION: Minimal Complete Button (全てのREQタスクが終わったら出現)
        val canShowBottomCompleteButton = allRequiredCompleted || (activeRequired.isEmpty() && totalRequired.isEmpty() && (completedItems.isNotEmpty() || currentPhase.isCompleted))
        if (canShowBottomCompleteButton) {
            val isLastPhase = selectedIndex >= state.phases.size - 1
            val buttonLabel = when {
                isLastPhase -> if (isEnglish) "COMPLETE DAY & RESTART" else "1日のタスクをすべて完了"
                currentPhase.isCompleted -> if (isEnglish) "PHASE COMPLETED (RE-EVALUATE)" else "フェーズ完了済み (再確認)"
                else -> if (isEnglish) "COMPLETE PHASE & ADVANCE" else "フェーズ完了して次へ"
            }

            val shouldInvertHighlight = allRequiredCompleted || allTasksCompleted

            SpaceXButton(
                text = buttonLabel,
                onClick = {
                    if (state.settings.confirmReviewOnAdvance) {
                        showReviewDialog = true
                    } else {
                        viewModel.completePhase(currentPhase.id, false, null)
                    }
                },
                prominent = !currentPhase.isCompleted || shouldInvertHighlight,
                inverted = shouldInvertHighlight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    }

    // REVIEW DIALOG ("問題なかった？")
    if (showReviewDialog) {
        ReviewDialog(
            phaseName = currentPhase.name,
            language = state.settings.appLanguage,
            onDismiss = { showReviewDialog = false },
            onConfirm = { hadIssue, note ->
                viewModel.completePhase(currentPhase.id, hadIssue, note)
                showReviewDialog = false
            }
        )
    }

    // EDIT ITEM DIALOG (タスク設定を常に実行可能)
    if (editingItem != null) {
        val item = editingItem!!
        EditItemDialog(
            item = item,
            language = state.settings.appLanguage,
            onDismiss = { editingItem = null },
            onSave = { updatedTitle, updatedPriority ->
                viewModel.updateItem(currentPhase.id, item.id, updatedTitle, updatedPriority)
                editingItem = null
            },
            onDelete = {
                viewModel.removeItem(currentPhase.id, item.id)
                editingItem = null
            }
        )
    }

    // APPLY TEMPLATE DIALOG
    if (showTemplateSelectDialog) {
        TemplateSelectDialog(
            templates = state.templates,
            language = state.settings.appLanguage,
            onDismiss = { showTemplateSelectDialog = false },
            onSelect = { templateId ->
                viewModel.applyTemplate(currentPhase.id, templateId, replace = false)
                showTemplateSelectDialog = false
            }
        )
    }

    // RESET DEFAULT CONFIRMATION (不可逆操作防止ダイアログ)
    if (showResetDefaultDialog) {
        SpaceXConfirmDialog(
            title = if (isEnglish) "CONFIRM RESET: 4 PHASES" else "4フェーズ初期化の確認",
            message = if (isEnglish) 
                "Are you sure you want to reset all phases to the default 4 phases (Morning/Noon/Afternoon/Night)? This cannot be undone."
            else 
                "すべてのフェーズとタスクを初期の4フェーズ構成 (朝/午前/午後/夜) にリセットしますか？この操作は取り消せません。",
            confirmText = if (isEnglish) "RESET" else "リセット",
            onConfirm = {
                viewModel.restoreDefaultFourPhases()
                showResetDefaultDialog = false
            },
            onDismiss = { showResetDefaultDialog = false }
        )
    }

    // RESET PHASE CONFIRMATION (不可逆操作防止ダイアログ)
    if (showResetPhaseDialog) {
        SpaceXConfirmDialog(
            title = if (isEnglish) "CONFIRM RESET: ${currentPhase.name.uppercase()}" else "フェーズ初期化: ${currentPhase.name}",
            message = if (isEnglish) 
                "Reset all task completions in '${currentPhase.name}' back to uncompleted?"
            else 
                "現在のフェーズ「${currentPhase.name}」のすべてのチェック状態を未完了にリセットしますか？",
            confirmText = if (isEnglish) "RESET" else "リセット",
            onConfirm = {
                viewModel.resetPhase(currentPhase.id)
                showResetPhaseDialog = false
            },
            onDismiss = { showResetPhaseDialog = false }
        )
    }

    // CLEAR COMPLETED CONFIRMATION (不可逆操作防止ダイアログ)
    if (showClearCompletedDialog) {
        SpaceXConfirmDialog(
            title = if (isEnglish) "CLEAR COMPLETED: ${currentPhase.name.uppercase()}" else "完了タスクの消去: ${currentPhase.name}",
            message = if (isEnglish) 
                "Delete all completed tasks in this phase?"
            else 
                "このフェーズ内の完了済みタスクを一括削除しますか？",
            confirmText = if (isEnglish) "CLEAR" else "消去",
            onConfirm = {
                viewModel.clearCompletedItems(currentPhase.id)
                showClearCompletedDialog = false
            },
            onDismiss = { showClearCompletedDialog = false }
        )
    }
}

// CHECKLIST ITEM ROW (編集・長押しドラッグ並び替え対応)
/**
 * タスク並び替え用の長押しドラッグジェスチャー検出
 * 250msの長押しでカードをハイライトし、親のスクロールに奪われず確実にドラッグ可能にする
 */
private suspend fun PointerInputScope.detectTaskDragAfterLongPress(
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val longPressTimeout = 220L
        val longPressCancelled = withTimeoutOrNull(longPressTimeout) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null || !change.pressed) {
                    return@withTimeoutOrNull true
                }
                val distance = (change.position - down.position).getDistance()
                if (distance > 40f) {
                    return@withTimeoutOrNull true
                }
            }
            @Suppress("UNREACHABLE_CODE")
            false
        }

        // タイムアウトした（= 220ms間、微小なブレ内で指が置かれた）場合、長押し成立！
        if (longPressCancelled == null) {
            down.consume()
            onDragStart()

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null || !change.pressed) {
                    onDragEnd()
                    break
                }
                val deltaY = change.position.y - change.previousPosition.y
                change.consume()
                onDragDelta(deltaY)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChecklistItemRow(
    item: ChecklistItem,
    isCritical: Boolean,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    dragOffsetY: Float = 0f,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: (() -> Unit)? = null,
    onDragDelta: ((Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null
) {
    val isCompleted = item.isCompleted
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragDelta by rememberUpdatedState(onDragDelta)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    val boxModifier = if (isDragging) {
        modifier
            .fillMaxWidth()
            .zIndex(15f)
            .graphicsLayer {
                translationY = dragOffsetY
                scaleX = 1.035f
                scaleY = 1.035f
                shadowElevation = 20f
            }
            .border(2.dp, com.example.checklist.theme.SpaceXAccent, SpaceXShape)
            .background(SurfaceElevated, shape = SpaceXShape)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    } else if (isCompleted) {
        // 完了済みカード: 枠を消して行だけの軽い表示にし、未完了タスクを際立たせる
        modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    } else {
        // 未完了カード: REQならばテーマアクセント枠(SpaceXAccent: 白/赤/緑)、NORMならば現行(BorderSubtle)
        modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isCritical) com.example.checklist.theme.SpaceXAccent else BorderSubtle,
                shape = SpaceXShape
            )
            .background(
                if (isCritical) SurfaceElevated else SpaceBlack,
                shape = SpaceXShape
            )
            .padding(horizontal = 6.dp, vertical = 6.dp)
    }

    Box(modifier = boxModifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reorder controls: 左端の長押しドラッグハンドル（クリックイベントと完全分離）
            if (onDragStart != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .pointerInput(item.id) {
                            detectTaskDragAfterLongPress(
                                onDragStart = { currentOnDragStart?.invoke() },
                                onDragDelta = { deltaY -> currentOnDragDelta?.invoke(deltaY) },
                                onDragEnd = { currentOnDragEnd?.invoke() }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "長押しして並び替え",
                        tint = if (isDragging) com.example.checklist.theme.SpaceXAccent else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Spacer(modifier = Modifier.width(6.dp))
            }

            // タスクコンテンツ領域: 誤タッチ防止のためチェックボックスとテキストを分離
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SpaceX Wireframe Checkbox (チェックボックス部分タップでのみ完了トグル: 航空の厳格チェック思想)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(onClick = onTap),
                    contentAlignment = Alignment.Center
                ) {
                    SpaceXCheckbox(
                        checked = isCompleted,
                        size = 24.dp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Title and Carryover Info (テキストタップ時は誤完了せず、タスク編集ダイアログを開く)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onEdit)
                ) {
                    if (item.isCarryover) {
                        Text(
                            text = "CARRYOVER // ${item.sourcePhaseName ?: "PREVIOUS"}",
                            color = TextSecondary,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(1.dp)) // 行間を半減
                    }
                    Text(
                        text = formatTaskText(item.title),
                        color = when {
                            isCompleted -> TextTertiary
                            isCritical -> TextPrimary
                            else -> TextPrimary
                        },
                        fontSize = if (isCritical) 14.sp else 13.5.sp,
                        fontWeight = if (isCritical) FontWeight.Bold else FontWeight.Normal,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                        letterSpacing = 0.3.sp,
                        style = TextStyle(lineBreak = SpaceXLineBreak)
                    )
                }
            }

            // Delete action (テキストタップで編集可能なため鉛筆アイコンは廃止し、右端に安全なタッチ領域で削除ボタンを配置)
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete item",
                    tint = TextTertiary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

// DISCARDED ITEM ROW (破棄されたタスクの保管と復元)
@Composable
fun DiscardedItemRow(
    item: ChecklistItem,
    onRestore: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, SpaceXShape)
            .background(SurfaceDark, shape = SpaceXShape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TRASH",
                color = TextDisabled,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = formatTaskText(item.title),
                color = TextDisabled,
                fontSize = 12.sp,
                textDecoration = TextDecoration.LineThrough,
                modifier = Modifier.weight(1f),
                style = TextStyle(lineBreak = SpaceXLineBreak)
            )

            SpaceXTextButton(
                text = "RESTORE",
                onClick = onRestore
            )
        }
    }
}

// EDIT ITEM DIALOG (タスクのタイトル・優先度の常時編集)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditItemDialog(
    item: ChecklistItem,
    language: AppLanguage = AppLanguage.JA,
    onDismiss: () -> Unit,
    onSave: (title: String, priority: Priority) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var priority by remember { mutableStateOf(item.priority) }
    val isTitleValid = title.trim().isNotEmpty()

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
                    text = AppStrings.taskConfig(language),
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
                        value = title,
                        onValueChange = { title = it },
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif
                        ),
                        cursorBrush = SolidColor(TextPrimary),
                        modifier = Modifier.fillMaxSize(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (isTitleValid) onSave(title.trim(), priority)
                        })
                    )
                    if (title.isEmpty()) {
                        Text(
                            text = AppStrings.taskTitleHint(language),
                            color = TextTertiary,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Priority Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isReq = priority == Priority.REQUIRED
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, if (isReq) com.example.checklist.theme.SpaceXAccent else BorderSubtle, SpaceXSmallShape)
                            .background(if (isReq) SurfaceElevated else SpaceBlack, shape = SpaceXSmallShape)
                            .combinedClickable(onClick = { priority = Priority.REQUIRED })
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (language == AppLanguage.EN) "CRITICAL (CRIT)" else "重要 (CRIT)",
                            color = if (isReq) com.example.checklist.theme.SpaceXAccent else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isReq) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    val isNorm = priority == Priority.NORMAL
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, if (isNorm) TextPrimary else BorderSubtle, SpaceXSmallShape)
                            .background(if (isNorm) SurfaceElevated else SpaceBlack, shape = SpaceXSmallShape)
                            .combinedClickable(onClick = { priority = Priority.NORMAL })
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (language == AppLanguage.EN) "STANDARD (STD)" else "標準 (STD)",
                            color = if (isNorm) TextPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isNorm) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpaceXTextButton(
                        text = AppStrings.delete(language),
                        onClick = onDelete
                    )

                    Row {
                        SpaceXTextButton(text = AppStrings.cancel(language), onClick = onDismiss)
                        Spacer(modifier = Modifier.width(8.dp))
                        SpaceXButton(
                            text = AppStrings.save(language),
                            onClick = {
                                if (isTitleValid) onSave(title.trim(), priority)
                            },
                            enabled = isTitleValid
                        )
                    }
                }
            }
        }
    }
}

// REVIEW DIALOG ("ALL SYSTEMS NOMINAL?")
@Composable
fun ReviewDialog(
    phaseName: String,
    language: AppLanguage = AppLanguage.JA,
    onDismiss: () -> Unit,
    onConfirm: (hadIssue: Boolean, note: String?) -> Unit
) {
    var step by remember { mutableStateOf<ReviewStep>(ReviewStep.Question) }
    var noteText by remember { mutableStateOf("") }

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
                    text = AppStrings.reviewTitle(language, phaseName),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = AppStrings.reviewQuestion(language),
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                when (step) {
                    ReviewStep.Question -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SpaceXButton(
                                text = AppStrings.reviewYes(language),
                                onClick = { onConfirm(false, null) },
                                prominent = true,
                                modifier = Modifier.weight(1f)
                            )
                            SpaceXButton(
                                text = AppStrings.reviewNo(language),
                                onClick = { step = ReviewStep.NoteInput },
                                prominent = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    ReviewStep.NoteInput -> {
                        Text(
                            text = AppStrings.reviewNoteTitle(language),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .border(1.dp, BorderSubtle, SpaceXShape)
                                .background(SurfaceDark, shape = SpaceXShape)
                                .padding(10.dp)
                        ) {
                            BasicTextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                textStyle = TextStyle(
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.SansSerif
                                ),
                                cursorBrush = SolidColor(TextPrimary),
                                modifier = Modifier.fillMaxSize()
                            )
                            if (noteText.isEmpty()) {
                                Text(
                                    text = AppStrings.reviewNoteHint(language),
                                    color = TextTertiary,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SpaceXButton(
                                text = AppStrings.recordAndComplete(language),
                                onClick = { onConfirm(true, noteText.trim().ifEmpty { null }) },
                                prominent = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            SpaceXTextButton(
                                text = AppStrings.cancel(language),
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

private sealed interface ReviewStep {
    object Question : ReviewStep
    object NoteInput : ReviewStep
}

// TEMPLATE SELECT DIALOG
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TemplateSelectDialog(
    templates: List<com.example.checklist.data.PhaseTemplate>,
    language: AppLanguage = AppLanguage.JA,
    onDismiss: () -> Unit,
    onSelect: (templateId: String) -> Unit
) {
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
                    text = AppStrings.applyTemplateTitle(language),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (templates.isEmpty()) {
                    Text(
                        text = AppStrings.noTemplatesAvailable(language),
                        color = TextTertiary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(templates) { t ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderSubtle, SpaceXShape)
                                    .background(SurfaceDark, shape = SpaceXShape)
                                    .combinedClickable(onClick = { onSelect(t.id) })
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = t.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text(text = AppStrings.itemsCount(language, t.items.size), color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Text(text = AppStrings.apply(language), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    SpaceXTextButton(text = AppStrings.close(language), onClick = onDismiss)
                }
            }
        }
    }
}
