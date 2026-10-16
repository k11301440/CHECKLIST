package com.example.checklist.ui.phase

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.checklist.data.AppState
import com.example.checklist.data.ChecklistItem
import com.example.checklist.data.Phase
import com.example.checklist.data.Priority
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
import com.example.checklist.ui.components.SpaceXTextButton
import java.time.LocalDate
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

    // 常時入力用のステート（ダイアログなしでいつでも直接入力可能）
    var quickInputTitle by remember { mutableStateOf("") }
    var quickInputPriority by remember { mutableStateOf(Priority.NORMAL) }
    var quickTargetPhaseIndex by remember { mutableStateOf(selectedIndex) }
    var quickTargetDayOffset by remember { mutableStateOf(0) } // 0: 今日, 1: 明日, 2: 2日後
    var showQuickOptions by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // ROW 1: TOP NAVIGATION (Calendar / Home / Settings / Menu)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FLIGHT CHECKLIST",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

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
                            .background(SurfaceDark)
                            .border(1.dp, BorderProminent, RoundedCornerShape(2.dp))
                    ) {
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
                            text = { Text("現在のリストをテンプレート保存", color = TextPrimary, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                viewModel.savePhaseAsTemplate(currentPhase.id, "${currentPhase.name} テンプレート")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("このフェーズをリセット", color = TextPrimary, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                showResetPhaseDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("設定", color = TextSecondary, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                onNavigateSettings()
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
                            color = if (isSelected) TextPrimary else BorderSubtle,
                            shape = RoundedCornerShape(2.dp)
                        )
                        .background(if (isSelected) SurfaceElevated else SpaceBlack)
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

        SpaceXDivider()

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
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
                Spacer(modifier = Modifier.height(2.dp))
                val statusText = if (remainingRequiredCount > 0) {
                    "$remainingRequiredCount REQUIRED PENDING"
                } else if (totalRequired.isNotEmpty()) {
                    "ALL REQUIRED CHECKED"
                } else {
                    "NO REQUIRED ITEMS"
                }
                Text(
                    text = statusText,
                    color = if (remainingRequiredCount > 0) TextSecondary else TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp
                )
            }

            // 完了済み単発タスクのクリアボタン（完了項目がある場合すぐに押せる）
            if (completedTotalCount > 0) {
                SpaceXTextButton(
                    text = "CLEAR DONE ($completedTotalCount)",
                    onClick = { viewModel.clearCompletedItems(currentPhase.id) },
                    prominent = false
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CHECKLIST ITEMS
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ACTIVE TASKS (REQ / NORM を単一リストとして描画し、自由に相互並び替え可能)
            if (activeItems.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE TASKS (${activeItems.size})",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "左端をドラッグして上下に並び替え",
                            color = TextTertiary,
                            fontSize = 9.5.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                itemsIndexed(activeItems, key = { _, item -> item.id }) { activeIndex, item ->
                    val indexInPhase = currentPhase.items.indexOfFirst { it.id == item.id }
                    ChecklistItemRow(
                        item = item,
                        isCritical = item.priority == Priority.REQUIRED,
                        onTap = { viewModel.completeItem(currentPhase.id, item.id) },
                        onLongPress = { viewModel.undoItem(currentPhase.id, item.id) },
                        onEdit = { editingItem = item },
                        onDelete = { viewModel.removeItem(currentPhase.id, item.id) },
                        onMoveUp = if (activeIndex > 0) {
                            val prevItem = activeItems[activeIndex - 1]
                            val prevPhaseIndex = currentPhase.items.indexOfFirst { it.id == prevItem.id }
                            val action: () -> Unit = { viewModel.reorderItems(currentPhase.id, indexInPhase, prevPhaseIndex) }
                            action
                        } else null,
                        onMoveDown = if (activeIndex < activeItems.size - 1) {
                            val nextItem = activeItems[activeIndex + 1]
                            val nextPhaseIndex = currentPhase.items.indexOfFirst { it.id == nextItem.id }
                            val action: () -> Unit = { viewModel.reorderItems(currentPhase.id, indexInPhase, nextPhaseIndex) }
                            action
                        } else null,
                        onDragSwap = { direction ->
                            if (direction < 0 && activeIndex > 0) {
                                val prevItem = activeItems[activeIndex - 1]
                                val prevPhaseIndex = currentPhase.items.indexOfFirst { it.id == prevItem.id }
                                viewModel.reorderItems(currentPhase.id, indexInPhase, prevPhaseIndex)
                            } else if (direction > 0 && activeIndex < activeItems.size - 1) {
                                val nextItem = activeItems[activeIndex + 1]
                                val nextPhaseIndex = currentPhase.items.indexOfFirst { it.id == nextItem.id }
                                viewModel.reorderItems(currentPhase.id, indexInPhase, nextPhaseIndex)
                            }
                        }
                    )
                }
            }

            // COMPLETED & ARCHIVED SECTION (完了したタスクと保管された削除タスクを下に移動)
            if (completedItems.isNotEmpty() || discardedItems.isNotEmpty()) {
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
                            text = "COMPLETED & ARCHIVED (${completedItems.size + discardedItems.size})",
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
                    items(completedItems, key = { it.id }) { item ->
                        ChecklistItemRow(
                            item = item,
                            isCritical = item.priority == Priority.REQUIRED,
                            onTap = { /* 完了済み */ },
                            onLongPress = { viewModel.undoItem(currentPhase.id, item.id) },
                            onEdit = { editingItem = item },
                            onDelete = { viewModel.removeItem(currentPhase.id, item.id) }
                        )
                    }

                    // 破棄タスク（誤破棄防止のため保管・ワンタップ復元可能）
                    items(discardedItems, key = { it.id }) { item ->
                        DiscardedItemRow(
                            item = item,
                            onRestore = { viewModel.restoreItem(currentPhase.id, item.id) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // ALWAYS-AVAILABLE QUICK TASK BAR (仕事用の単発タスクをいつでも即追加)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                    .background(SurfaceDark)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority switch (REQ / NORM) - 四角の大きさを固定幅(54dp)で統一
                Box(
                    modifier = Modifier
                        .width(54.dp)
                        .border(
                            width = 1.dp,
                            color = if (quickInputPriority == Priority.REQUIRED) TextPrimary else BorderSubtle,
                            shape = RoundedCornerShape(2.dp)
                        )
                        .background(if (quickInputPriority == Priority.REQUIRED) SurfaceElevated else SpaceBlack)
                        .combinedClickable(onClick = {
                            quickInputPriority = if (quickInputPriority == Priority.REQUIRED) Priority.NORMAL else Priority.REQUIRED
                        })
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (quickInputPriority == Priority.REQUIRED) "REQ" else "NORM",
                        color = if (quickInputPriority == Priority.REQUIRED) TextPrimary else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
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
                        keyboardActions = KeyboardActions(onDone = {
                            if (quickInputTitle.isNotBlank()) {
                                val targetPhase = state.phases.getOrNull(quickTargetPhaseIndex) ?: currentPhase
                                val targetDate = if (quickTargetDayOffset > 0) {
                                    val base = runCatching { LocalDate.parse(state.currentDate) }.getOrElse { LocalDate.now() }
                                    base.plusDays(quickTargetDayOffset.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
                                } else null
                                viewModel.addItem(targetPhase.id, quickInputTitle, quickInputPriority, scheduledDate = targetDate)
                                quickInputTitle = ""
                                showQuickOptions = false
                            }
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (quickInputTitle.isEmpty()) {
                        Text(
                            text = "新しいタスクを入力...",
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
                        tint = if (quickTargetDayOffset > 0 || quickTargetPhaseIndex != selectedIndex) TextPrimary else TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Add button: 紙飛行機アイコン (Send)
                IconButton(
                    onClick = {
                        if (quickInputTitle.isNotBlank()) {
                            val targetPhase = state.phases.getOrNull(quickTargetPhaseIndex) ?: currentPhase
                            val targetDate = if (quickTargetDayOffset > 0) {
                                val base = runCatching { LocalDate.parse(state.currentDate) }.getOrElse { LocalDate.now() }
                                base.plusDays(quickTargetDayOffset.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
                            } else null
                            viewModel.addItem(targetPhase.id, quickInputTitle, quickInputPriority, scheduledDate = targetDate)
                            quickInputTitle = ""
                            showQuickOptions = false
                        }
                    },
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
                        .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                        .background(SurfaceDark)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "SCHEDULE // 開始日・フェーズ選択",
                        color = TextTertiary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // 日付選択
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val dayLabels = listOf("今日 (TODAY)" to 0, "明日 (TOMORROW)" to 1, "+2日後" to 2)
                        for ((label, offset) in dayLabels) {
                            val isSelected = quickTargetDayOffset == offset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, if (isSelected) TextPrimary else BorderSubtle, RoundedCornerShape(2.dp))
                                    .background(if (isSelected) SurfaceElevated else SpaceBlack)
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
                                    .border(1.dp, if (isSelected) TextPrimary else BorderSubtle, RoundedCornerShape(2.dp))
                                    .background(if (isSelected) SurfaceElevated else SpaceBlack)
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

        // BOTTOM ACTION: Minimal Complete Button (タスク全クリア時は色を反転して強調)
        val isLastPhase = selectedIndex >= state.phases.size - 1
        val buttonLabel = when {
            isLastPhase -> "COMPLETE DAY // ADVANCE TO MORNING"
            currentPhase.isCompleted -> "PHASE COMPLETED // RE-EVALUATE"
            else -> "COMPLETE PHASE // ADVANCE"
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

    // REVIEW DIALOG ("問題なかった？")
    if (showReviewDialog) {
        ReviewDialog(
            phaseName = currentPhase.name,
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
            title = "CONFIRM RESET // 4 PHASES",
            message = "すべてのフェーズとタスクを初期の4フェーズ構成 (朝/午前/午後/夜) にリセットしますか？この操作は取り消せません。",
            confirmText = "RESET",
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
            title = "CONFIRM RESET // ${currentPhase.name.uppercase()}",
            message = "現在のフェーズ「${currentPhase.name}」のすべてのチェック状態を未完了にリセットしますか？",
            confirmText = "RESET",
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
            title = "CLEAR COMPLETED // ${currentPhase.name.uppercase()}",
            message = "このフェーズ内の完了済みタスクを一括削除しますか？",
            confirmText = "CLEAR",
            onConfirm = {
                viewModel.clearCompletedItems(currentPhase.id)
                showClearCompletedDialog = false
            },
            onDismiss = { showClearCompletedDialog = false }
        )
    }
}

// CHECKLIST ITEM ROW (編集・並び替え可能)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChecklistItemRow(
    item: ChecklistItem,
    isCritical: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onDragSwap: ((Int) -> Unit)? = null
) {
    val isCompleted = item.isCompleted

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isCritical && !isCompleted) BorderProminent else BorderSubtle,
                shape = RoundedCornerShape(2.dp)
            )
            .background(if (isCritical && !isCompleted) SurfaceElevated else SpaceBlack)
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            )
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reorder controls (ドラッグハンドル & 上下微調整ボタン)
            if (onDragSwap != null || onMoveUp != null || onMoveDown != null) {
                var dragAccumulated by remember { mutableFloatStateOf(0f) }
                val thresholdPx = with(LocalDensity.current) { 40.dp.toPx() }

                Row(
                    modifier = Modifier.padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .pointerInput(item.id) {
                                detectDragGestures(
                                    onDragStart = { dragAccumulated = 0f },
                                    onDragEnd = { dragAccumulated = 0f },
                                    onDragCancel = { dragAccumulated = 0f },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragAccumulated += dragAmount.y
                                        if (dragAccumulated > thresholdPx) {
                                            onDragSwap?.invoke(1)
                                            dragAccumulated = 0f
                                        } else if (dragAccumulated < -thresholdPx) {
                                            onDragSwap?.invoke(-1)
                                            dragAccumulated = 0f
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.width(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = { onMoveUp?.invoke() },
                            enabled = onMoveUp != null,
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move up",
                                tint = if (onMoveUp != null) TextSecondary else BorderSubtle,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(
                            onClick = { onMoveDown?.invoke() },
                            enabled = onMoveDown != null,
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move down",
                                tint = if (onMoveDown != null) TextSecondary else BorderSubtle,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Visual distinction for critical items: Solid left vertical line
            if (isCritical) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(22.dp)
                        .background(if (isCompleted) TextTertiary else TextPrimary)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // SpaceX Wireframe Checkbox (大きめ 24.dp)
            SpaceXCheckbox(
                checked = isCompleted,
                size = 24.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Title and Priority Badges
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isCritical) "REQ" else "NORM",
                        color = if (isCritical) TextPrimary else TextTertiary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier
                            .border(1.dp, if (isCritical) TextPrimary else BorderSubtle, RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                    if (item.isCarryover) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CARRYOVER // ${item.sourcePhaseName ?: "PREVIOUS"}",
                            color = TextSecondary,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
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
                if (isCompleted) {
                    Text(
                        text = "長押しで解除",
                        color = TextDisabled,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Edit icon (タスク設定を常に実行可能)
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit task",
                    tint = TextTertiary,
                    modifier = Modifier.size(13.dp)
                )
            }

            // Delete action (完了セクションに保管されるため誤破棄防止)
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete item",
                    tint = TextTertiary,
                    modifier = Modifier.size(14.dp)
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
            .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
            .background(SurfaceDark)
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
    onDismiss: () -> Unit,
    onSave: (title: String, priority: Priority) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var priority by remember { mutableStateOf(item.priority) }

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
                    text = "TASK CONFIG",
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
                        value = title,
                        onValueChange = { title = it },
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
                            .border(1.dp, if (isReq) TextPrimary else BorderSubtle, RoundedCornerShape(2.dp))
                            .background(if (isReq) SurfaceElevated else SpaceBlack)
                            .combinedClickable(onClick = { priority = Priority.REQUIRED })
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "必須 (CRITICAL)",
                            color = if (isReq) TextPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isReq) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    val isNorm = priority == Priority.NORMAL
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, if (isNorm) TextPrimary else BorderSubtle, RoundedCornerShape(2.dp))
                            .background(if (isNorm) SurfaceElevated else SpaceBlack)
                            .combinedClickable(onClick = { priority = Priority.NORMAL })
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "普通 (NORMAL)",
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
                        text = "DELETE",
                        onClick = onDelete
                    )

                    Row {
                        SpaceXTextButton(text = "CANCEL", onClick = onDismiss)
                        Spacer(modifier = Modifier.width(8.dp))
                        SpaceXButton(
                            text = "SAVE",
                            onClick = {
                                if (title.isNotBlank()) onSave(title, priority)
                            },
                            enabled = title.isNotBlank()
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
    onDismiss: () -> Unit,
    onConfirm: (hadIssue: Boolean, note: String?) -> Unit
) {
    var step by remember { mutableStateOf<ReviewStep>(ReviewStep.Question) }
    var noteText by remember { mutableStateOf("") }

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
                    text = "PHASE REVIEW // ${phaseName.uppercase()}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "ALL SYSTEMS NOMINAL?",
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
                                text = "YES",
                                onClick = { onConfirm(false, null) },
                                prominent = true,
                                modifier = Modifier.weight(1f)
                            )
                            SpaceXButton(
                                text = "NO",
                                onClick = { step = ReviewStep.NoteInput },
                                prominent = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    ReviewStep.NoteInput -> {
                        Text(
                            text = "振り返りメモ (任意)",
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
                                .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                                .background(SurfaceDark)
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
                                    text = "気づきや課題、ブロッカーを入力...",
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
                                text = "RECORD & COMPLETE",
                                onClick = { onConfirm(true, noteText) },
                                prominent = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            SpaceXTextButton(
                                text = "CANCEL",
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
    onDismiss: () -> Unit,
    onSelect: (templateId: String) -> Unit
) {
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
                    text = "APPLY TEMPLATE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (templates.isEmpty()) {
                    Text(
                        text = "利用可能なテンプレートがありません",
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
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                                    .background(SurfaceDark)
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
                                        Text(text = "${t.items.size} 項目", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Text(text = "適用", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    SpaceXTextButton(text = "CLOSE", onClick = onDismiss)
                }
            }
        }
    }
}
