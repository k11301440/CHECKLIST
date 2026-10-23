package com.example.checklist.ui.phase

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlinx.coroutines.withTimeoutOrNull
import com.example.checklist.ui.components.SpaceXConfirmDialog
import com.example.checklist.ui.components.LocalLargeButtonMode
import com.example.checklist.ui.components.LocalUiSize
import com.example.checklist.ui.components.uiDimension
import com.example.checklist.ui.components.uiFontSize
import com.example.checklist.ui.components.scaledWithFont
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import com.example.checklist.data.TaskSync
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
import com.example.checklist.ui.components.LocalLargeButtonMode
import com.example.checklist.ui.components.LocalUiSize
import com.example.checklist.ui.components.uiDimension
import com.example.checklist.ui.components.uiFontSize
import com.example.checklist.ui.components.scaledWithFont
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
    var showNextDaySwipeDialog by remember { mutableStateOf(false) }
    var showPrevDaySwipeDialog by remember { mutableStateOf(false) }

    var swipeDragTotalX by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = with(LocalDensity.current) { 70.dp.toPx() }

    val swipePhaseModifier = if (state.settings.enableSwipePhase) {
        Modifier.pointerInput(selectedIndex, state.phases.size) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (swipeDragTotalX < -swipeThresholdPx) {
                        // Swiped Left -> Advance to next
                        if (selectedIndex < state.phases.size - 1) {
                            viewModel.selectPhase(selectedIndex + 1)
                        } else {
                            showNextDaySwipeDialog = true
                        }
                    } else if (swipeDragTotalX > swipeThresholdPx) {
                        // Swiped Right -> Go to previous
                        if (selectedIndex > 0) {
                            viewModel.selectPhase(selectedIndex - 1)
                        } else {
                            showPrevDaySwipeDialog = true
                        }
                    }
                    swipeDragTotalX = 0f
                },
                onDragCancel = {
                    swipeDragTotalX = 0f
                },
                onHorizontalDrag = { _, dragAmount ->
                    swipeDragTotalX += dragAmount
                }
            )
        }
    } else {
        Modifier
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isLarge = LocalLargeButtonMode.current

    // ドラッグ＆ドロップ並び替え用ステート
    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val itemHeightPx = with(LocalDensity.current) { uiDimension(58.dp, 66.dp, 76.dp).toPx() }
    val currentAppState by rememberUpdatedState(state)

    // 現在時刻に基づくフェーズの動的判定 (定期チェックで画面を開いたままでも時刻変化に追従)
    var timeTick by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0L) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(15_000L)
            timeTick = System.currentTimeMillis()
            viewModel.checkAndAdvanceByTime()
        }
    }

    val currentPhaseIndexByTime = remember(state.phases, state.settings.phaseSchedules, selectedIndex, state.currentDate, timeTick) {
        TaskSync.computeCurrentPhaseIndexByTime(state, LocalTime.now())
    }

    // 常時入力用のステート（ダイアログなしでいつでも直接入力可能）
    // 表示中のフェーズ (selectedIndex) に追従させ、フェーズ移動後にタスク追加した際に別フェーズへ入らないようにする
    var quickInputTitle by remember { mutableStateOf("") }
    var quickInputPriority by remember { mutableStateOf(Priority.NORMAL) }
    var quickTargetPhaseIndex by remember(selectedIndex) { mutableStateOf(selectedIndex) }
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
                val navButtonSize = uiDimension(34.dp, 38.dp, 44.dp).scaledWithFont()
                val navIconSize = uiDimension(18.dp, 20.dp, 22.dp).scaledWithFont()

                IconButton(
                    onClick = onNavigateCalendar,
                    modifier = Modifier.size(navButtonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Flight Calendar",
                        tint = TextSecondary,
                        modifier = Modifier.size(navIconSize)
                    )
                }

                IconButton(
                    onClick = onNavigateHome,
                    modifier = Modifier.size(navButtonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home Overview",
                        tint = TextSecondary,
                        modifier = Modifier.size(navIconSize)
                    )
                }

                IconButton(
                    onClick = onNavigateSettings,
                    modifier = Modifier.size(navButtonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(navIconSize)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(navButtonSize)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = TextSecondary,
                            modifier = Modifier.size(navIconSize)
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
                            if (isEnglish) "Advance to Next Phase" else "次のフェーズへ"
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
                        .padding(vertical = uiDimension(7.dp, 9.dp, 12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = uiFontSize(11.sp, 12.5.sp, 14.sp),
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

        // 現時刻のフェーズまたは今日の日付でない場合に表示する「現在のフェーズに戻る」ボタン (同日の先取り完了を除く)
        val actualTodayStr = remember(state.currentDate, selectedIndex, currentPhaseIndexByTime, timeTick) {
            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
        val isActualToday = state.currentDate.isEmpty() || state.currentDate == actualTodayStr
        val timePhase = state.phases.getOrNull(currentPhaseIndexByTime)
        val isAheadDueToCleared = isActualToday && TaskSync.isAheadBecauseCompleted(
            phases = state.phases,
            selectedPhaseIndex = selectedIndex,
            currentPhaseIndexByTime = currentPhaseIndexByTime
        )
        val showReturnToTimePhase = (!isActualToday || selectedIndex != currentPhaseIndexByTime) && !isAheadDueToCleared

        // PHASE STATUS HEADER
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$todayStr // PHASE 0${selectedIndex + 1}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = currentPhase.name.uppercase(),
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    lineHeight = 36.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            // STATUS TEXT & ACTION BUTTON (完全に同じ横一列で垂直中央揃え・1行コンパクト表示)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusText = if (remainingRequiredCount > 0) {
                    if (isEnglish) "$remainingRequiredCount CRITICAL PENDING" else "重要タスク残り $remainingRequiredCount 件"
                } else if (totalRequired.isNotEmpty()) {
                    if (isEnglish) "NO PENDING TASKS" else "未処理のタスクなし"
                } else {
                    if (isEnglish) "NO CRITICAL ITEMS" else "重要タスクなし"
                }
                Text(
                    text = statusText,
                    color = if (remainingRequiredCount > 0) com.example.checklist.theme.SpaceXAccent else TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp,
                    maxLines = 1,
                    softWrap = false
                )

                // フェーズ名のすぐ下に1行でコンパクトに納まる「現在のフェーズに戻る」ボタン (日付・時刻ともに現在へジャンプ)
                if (showReturnToTimePhase && timePhase != null) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, com.example.checklist.theme.SpaceXAccent, SpaceXSmallShape)
                            .background(SurfaceDark, shape = SpaceXSmallShape)
                            .clickable { viewModel.returnToCurrentPhase() }
                            .padding(horizontal = uiDimension(8.dp, 10.dp, 12.dp), vertical = uiDimension(3.dp, 4.5.dp, 6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEnglish) "CURRENT PHASE" else "現在のフェーズに戻る",
                            color = com.example.checklist.theme.SpaceXAccent,
                            fontSize = uiFontSize(9.5.sp, 10.5.sp, 12.sp),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp,
                            lineHeight = uiFontSize(12.sp, 13.sp, 14.sp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // CHECKLIST ITEMS (フェーズ遷移時は軽やかな横スライド＋フェードアニメーション)
        AnimatedContent(
            targetState = selectedIndex,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> (width * 0.25f).toInt() } + fadeIn(androidx.compose.animation.core.tween(180)))
                        .togetherWith(slideOutHorizontally { width -> (-width * 0.25f).toInt() } + fadeOut(androidx.compose.animation.core.tween(180)))
                } else {
                    (slideInHorizontally { width -> (-width * 0.25f).toInt() } + fadeIn(androidx.compose.animation.core.tween(180)))
                        .togetherWith(slideOutHorizontally { width -> (width * 0.25f).toInt() } + fadeOut(androidx.compose.animation.core.tween(180)))
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(swipePhaseModifier),
            label = "PhaseSlide"
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
                                    text = "下部の入力バーから新しいタスクを追加するか、右上の設定 > テンプレートからプリセットを読み込んでください。",
                                    color = TextTertiary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
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
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .combinedClickable(onClick = { isCompletedExpanded = !isCompletedExpanded })
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
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

                            // 完了済み・破棄タスクの一括消去ボタンを「完了・破棄されたタスク」見出しの横に配置
                            val archiveTotalCount = completedForList.size + discardedForList.size
                            if (archiveTotalCount > 0) {
                                SpaceXTextButton(
                                    text = if (isEnglish) "CLEAR (${archiveTotalCount})" else "履歴消去 (${archiveTotalCount})",
                                    onClick = { showClearCompletedDialog = true },
                                    prominent = false
                                )
                            }
                        }
                    }

                    if (isCompletedExpanded) {
                        // 完了タスク (チェックボックスをタップで即座に復活可能)
                        items(completedForList, key = { it.id }) { item ->
                            ChecklistItemRow(
                                item = item,
                                isCritical = item.priority == Priority.REQUIRED,
                                onTap = { viewModel.undoItem(phaseForList.id, item.id) },
                                onLongPress = { viewModel.undoItem(phaseForList.id, item.id) },
                                onEdit = { editingItem = item },
                                onDelete = { viewModel.removeItem(phaseForList.id, item.id) }
                            )
                        }

                        // 破棄タスク（ワンタップ復元、または完全消去可能）
                        items(discardedForList, key = { it.id }) { item ->
                            DiscardedItemRow(
                                item = item,
                                onRestore = { viewModel.restoreItem(phaseForList.id, item.id) },
                                onPermanentDelete = { viewModel.permanentlyDeleteItem(phaseForList.id, item.id) }
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
                    // スケジュールオプションを開いて選んでいない限り、確実に「今開いているフェーズ」に追加
                    val targetIdx = if (showQuickOptions) quickTargetPhaseIndex else selectedIndex
                    val targetPhase = state.phases.getOrNull(targetIdx) ?: currentPhase
                    val base = runCatching { LocalDate.parse(state.currentDate) }.getOrElse { LocalDate.now() }
                    val targetDate = if (quickTargetDayOffset > 0) {
                        base.plusDays(quickTargetDayOffset.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
                    } else {
                        state.currentDate.ifEmpty { base.format(DateTimeFormatter.ISO_LOCAL_DATE) }
                    }
                    viewModel.addItem(targetPhase.id, quickInputTitle, quickInputPriority, scheduledDate = targetDate)
                    quickInputTitle = ""
                    quickTargetDayOffset = 0
                    quickTargetPhaseIndex = selectedIndex
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
                        .width(uiDimension(46.dp, 52.dp, 60.dp).scaledWithFont())
                        .border(
                            width = 1.dp,
                            color = if (quickInputPriority == Priority.REQUIRED) com.example.checklist.theme.SpaceXAccent else BorderSubtle,
                            shape = SpaceXSmallShape
                        )
                        .background(if (quickInputPriority == Priority.REQUIRED) SurfaceElevated else SpaceBlack, shape = SpaceXSmallShape)
                        .combinedClickable(onClick = {
                            quickInputPriority = if (quickInputPriority == Priority.REQUIRED) Priority.NORMAL else Priority.REQUIRED
                        })
                        .padding(vertical = uiDimension(4.dp, 5.5.dp, 7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = priorityLabel,
                        color = if (quickInputPriority == Priority.REQUIRED) com.example.checklist.theme.SpaceXAccent else TextSecondary,
                        fontSize = uiFontSize(10.sp, 11.sp, 12.sp),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Text field (デフォルトは現在見ているフェーズにタスクを追加)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = quickInputTitle,
                        onValueChange = {
                            quickInputTitle = it
                        },
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = uiFontSize(13.sp, 14.sp, 15.5.sp),
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
                            text = if (isEnglish) "Add task to ${currentPhase.name}..." else "「${currentPhase.name}」にタスクを追加...",
                            color = TextTertiary,
                            fontSize = uiFontSize(12.sp, 13.sp, 14.5.sp)
                        )
                    }
                }

                // Options toggle (いつ・どこに追加するか変更したい時のみ展開、文字サイズに連動拡大)
                IconButton(
                    onClick = {
                        showQuickOptions = !showQuickOptions
                        if (showQuickOptions) {
                            quickTargetPhaseIndex = selectedIndex
                            quickTargetDayOffset = 0
                        }
                    },
                    modifier = Modifier.size(uiDimension(30.dp, 34.dp, 38.dp).scaledWithFont())
                ) {
                    Icon(
                        imageVector = if (showQuickOptions) Icons.Default.ExpandLess else Icons.Default.DateRange,
                        contentDescription = "Schedule",
                        tint = if (showQuickOptions || quickTargetDayOffset > 0 || quickTargetPhaseIndex != selectedIndex) com.example.checklist.theme.SpaceXAccent else TextTertiary,
                        modifier = Modifier.size(uiDimension(16.dp, 18.dp, 20.dp).scaledWithFont())
                    )
                }

                // Add button: 紙飛行機アイコン (Send、文字サイズに連動拡大)
                IconButton(
                    onClick = { submitQuickTask() },
                    modifier = Modifier.size(uiDimension(32.dp, 36.dp, 40.dp).scaledWithFont()),
                    enabled = quickInputTitle.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Add Task",
                        tint = if (quickInputTitle.isNotBlank()) TextPrimary else TextTertiary,
                        modifier = Modifier.size(uiDimension(16.dp, 18.dp, 20.dp).scaledWithFont())
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
                else -> if (isEnglish) "ADVANCE TO NEXT PHASE" else "次のフェーズへ"
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
            title = if (isEnglish) "CLEAR ARCHIVE: ${currentPhase.name.uppercase()}" else "履歴消去: ${currentPhase.name}",
            message = if (isEnglish) 
                "Permanently delete all completed and discarded tasks in this phase?"
            else 
                "このフェーズ内の完了済みタスクおよび破棄されたタスクを完全に消去しますか？",
            confirmText = if (isEnglish) "CLEAR" else "消去",
            onConfirm = {
                viewModel.clearCompletedItems(currentPhase.id)
                showClearCompletedDialog = false
            },
            onDismiss = { showClearCompletedDialog = false }
        )
    }

    // 日跨ぎスワイプ時の確認ダイアログ
    if (showNextDaySwipeDialog) {
        SpaceXConfirmDialog(
            title = if (isEnglish) "ADVANCE TO NEXT DAY" else "翌日のフェーズへ進む",
            message = if (isEnglish)
                "Advance to the next day and start from Phase 01?"
            else
                "翌日に進み、最初のフェーズを開始しますか？",
            confirmText = if (isEnglish) "ADVANCE" else "進む",
            cancelText = if (isEnglish) "CANCEL" else "キャンセル",
            onConfirm = {
                showNextDaySwipeDialog = false
                viewModel.advanceToNextDay()
            },
            onDismiss = { showNextDaySwipeDialog = false }
        )
    }

    if (showPrevDaySwipeDialog) {
        val today = java.time.LocalDate.now()
        val curDate = runCatching { java.time.LocalDate.parse(state.currentDate) }.getOrDefault(today)
        val canGoBack = curDate.isAfter(today)
        SpaceXConfirmDialog(
            title = if (isEnglish) "GO TO PREVIOUS DAY" else "前日のフェーズへ戻る",
            message = if (canGoBack) {
                if (isEnglish) "Return to yesterday's final phase (${curDate.minusDays(1)})?" else "前日（${curDate.minusDays(1)}）の最終フェーズに戻りますか？"
            } else {
                if (isEnglish) "Cannot navigate prior to today. View past records in Calendar." else "本日より前には戻れません。過去の記録はカレンダーから確認できます。"
            },
            confirmText = if (canGoBack) (if (isEnglish) "RETURN" else "戻る") else "OK",
            cancelText = if (canGoBack) (if (isEnglish) "CANCEL" else "キャンセル") else "",
            onConfirm = {
                showPrevDaySwipeDialog = false
                if (canGoBack) {
                    val prevDate = curDate.minusDays(1).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                    viewModel.returnToPreviousDay(prevDate)
                }
            },
            onDismiss = { showPrevDaySwipeDialog = false }
        )
    }
}

// CHECKLIST ITEM ROW (編集・ドラッグ並び替え対応)
/**
 * タスク並び替え用のドラッグジェスチャー検出
 * ドラッグハンドル「≡」専用。触れて動かした瞬間に即座に親のスクロールを抑制し、スムーズな並び替えを実現する
 */
private suspend fun PointerInputScope.detectTaskHandleDrag(
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    detectDragGestures(
        onDragStart = {
            onDragStart()
        },
        onDrag = { change, dragAmount ->
            change.consume()
            onDragDelta(dragAmount.y)
        },
        onDragEnd = {
            onDragEnd()
        },
        onDragCancel = {
            onDragEnd()
        }
    )
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

    val cardVerticalPadding = uiDimension(4.dp, 6.dp, 8.dp)
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
            .padding(horizontal = 6.dp, vertical = cardVerticalPadding)
    } else if (isCompleted) {
        // 完了済みカード: 枠を消して行だけの軽い表示にし、未完了タスクを際立たせる
        modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 6.dp, vertical = uiDimension(3.dp, 5.dp, 6.dp))
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
            .padding(horizontal = 6.dp, vertical = cardVerticalPadding)
    }

    Box(modifier = boxModifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reorder controls: 左端のドラッグハンドル「≡」（触れて動かすだけで即座に並び替え、文字サイズに連動拡大）
            if (onDragStart != null) {
                Box(
                    modifier = Modifier
                        .size(uiDimension(32.dp, 38.dp, 44.dp).scaledWithFont())
                        .pointerInput(item.id) {
                            detectTaskHandleDrag(
                                onDragStart = { currentOnDragStart?.invoke() },
                                onDragDelta = { deltaY -> currentOnDragDelta?.invoke(deltaY) },
                                onDragEnd = { currentOnDragEnd?.invoke() }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "ドラッグして並び替え",
                        tint = if (isDragging) com.example.checklist.theme.SpaceXAccent else TextSecondary,
                        modifier = Modifier.size(uiDimension(18.dp, 21.dp, 24.dp).scaledWithFont())
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
                // SpaceX Wireframe Checkbox (チェックボックス部分タップでのみ完了トグル: 文字サイズに連動拡大)
                Box(
                    modifier = Modifier
                        .size(uiDimension(32.dp, 38.dp, 44.dp).scaledWithFont())
                        .clickable(onClick = onTap),
                    contentAlignment = Alignment.Center
                ) {
                    SpaceXCheckbox(
                        checked = isCompleted,
                        size = uiDimension(20.dp, 24.dp, 28.dp).scaledWithFont()
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
                            fontSize = uiFontSize(8.5.sp, 9.5.sp, 11.sp),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Text(
                        text = formatTaskText(item.title),
                        color = when {
                            isCompleted -> TextTertiary
                            isCritical -> TextPrimary
                            else -> TextPrimary
                        },
                        fontSize = if (isCritical) uiFontSize(14.sp, 15.5.sp, 17.5.sp) else uiFontSize(13.sp, 14.5.sp, 16.sp),
                        fontWeight = if (isCritical) FontWeight.Bold else FontWeight.Normal,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                        letterSpacing = 0.3.sp,
                        style = TextStyle(lineBreak = SpaceXLineBreak)
                    )
                }
            }

            // Delete action (未完了タスクのみ「×」ボタンを表示。完了タスクは「履歴消去」で完全消去するため不要)
            if (!isCompleted) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(uiDimension(30.dp, 35.dp, 40.dp).scaledWithFont())
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete item",
                        tint = TextTertiary,
                        modifier = Modifier.size(uiDimension(14.dp, 16.dp, 18.dp).scaledWithFont())
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(uiDimension(6.dp, 8.dp, 10.dp)))
            }
        }
    }
}

// DISCARDED ITEM ROW (破棄されたタスクの保管・復元・完全消去)
@Composable
fun DiscardedItemRow(
    item: ChecklistItem,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 6.dp, vertical = uiDimension(3.dp, 5.dp, 6.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TRASH",
                color = TextDisabled,
                fontSize = uiFontSize(9.sp, 10.sp, 11.sp),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = formatTaskText(item.title),
                color = TextDisabled,
                fontSize = uiFontSize(12.5.sp, 13.5.sp, 15.sp),
                textDecoration = TextDecoration.LineThrough,
                modifier = Modifier.weight(1f),
                style = TextStyle(lineBreak = SpaceXLineBreak)
            )

            SpaceXTextButton(
                text = "RESTORE",
                onClick = onRestore
            )

            Spacer(modifier = Modifier.width(6.dp))

            // 完全に破棄（見えなくさせる・永久削除）
            SpaceXTextButton(
                text = "DELETE",
                onClick = onPermanentDelete
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
