package com.example.checklist.ui.template

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.checklist.data.AppState
import com.example.checklist.data.PhaseTemplate
import com.example.checklist.data.Priority
import com.example.checklist.data.TemplateItem
import com.example.checklist.theme.BorderProminent
import com.example.checklist.theme.BorderSubtle
import com.example.checklist.theme.SpaceBlack
import com.example.checklist.theme.SurfaceDark
import com.example.checklist.theme.SurfaceElevated
import com.example.checklist.theme.TextPrimary
import com.example.checklist.theme.TextSecondary
import com.example.checklist.theme.TextTertiary
import com.example.checklist.ui.ChecklistViewModel
import com.example.checklist.ui.components.SpaceXButton
import com.example.checklist.ui.components.SpaceXDivider
import com.example.checklist.ui.components.SpaceXShape
import com.example.checklist.ui.components.SpaceXSmallShape
import com.example.checklist.ui.components.SpaceXTextButton
import java.util.UUID
import kotlinx.coroutines.withTimeoutOrNull

/**
 * テンプレート並び替え用の長押しドラッグジェスチャー検出
 */
private suspend fun PointerInputScope.detectTemplateDragAfterLongPress(
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
fun TemplateManageScreen(
    templateId: String?,
    state: AppState,
    viewModel: ChecklistViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val existingTemplate = remember(templateId, state.templates) {
        state.templates.find { it.id == templateId }
    }

    var templateName by remember {
        mutableStateOf(existingTemplate?.name ?: "新規テンプレート")
    }

    var selectedDays by remember {
        mutableStateOf(existingTemplate?.targetDaysOfWeek?.toSet() ?: emptySet())
    }

    val items = remember {
        mutableStateListOf<TemplateItem>().apply {
            if (existingTemplate != null) {
                addAll(existingTemplate.items)
            }
        }
    }

    var showAddItemDialog by remember { mutableStateOf(false) }
    var showPresetSuggestDialog by remember { mutableStateOf(existingTemplate == null && items.isEmpty()) }
    var selectedPresetIndex by remember { mutableStateOf(0) }
    val presetTemplates = remember { com.example.checklist.data.ChecklistRepository.getDefaultPresetTemplates() }

    // ドラッグ＆ドロップ並び替えステート
    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val itemHeightPx = with(LocalDensity.current) { 56.dp.toPx() }

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
                text = "TEMPLATE EDITOR",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            SpaceXTextButton(
                text = "SAVE",
                onClick = {
                    val finalTemplate = PhaseTemplate(
                        id = existingTemplate?.id ?: UUID.randomUUID().toString(),
                        name = templateName.trim().ifEmpty { "テンプレート" },
                        items = items.toList(),
                        targetDaysOfWeek = selectedDays.sorted()
                    )
                    viewModel.saveTemplate(finalTemplate)
                    onBack()
                },
                prominent = true
            )
        }

        SpaceXDivider()

        Spacer(modifier = Modifier.height(14.dp))

        // TEMPLATE NAME INPUT
        Text(
            text = "TEMPLATE NAME",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .border(1.dp, BorderSubtle, SpaceXShape)
                .background(SurfaceDark, shape = SpaceXShape)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = templateName,
                onValueChange = { templateName = it },
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                ),
                cursorBrush = SolidColor(TextPrimary),
                singleLine = true,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // RECURRENCE / DAY SCHEDULE OPTIONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "APPLY SCHEDULE (適用曜日オプション)",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = if (selectedDays.isEmpty()) "毎日 (全曜日)" else "${selectedDays.size} 曜日選択中",
                color = if (selectedDays.isNotEmpty()) com.example.checklist.theme.SpaceXAccent else TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Weekday Toggle Chips
        val weekdays = listOf(
            1 to "月", 2 to "火", 3 to "水", 4 to "木", 5 to "金", 6 to "土", 7 to "日"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            weekdays.forEach { (dayNum, dayLabel) ->
                val isSelected = dayNum in selectedDays
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) com.example.checklist.theme.SpaceXAccent else BorderSubtle,
                            shape = SpaceXSmallShape
                        )
                        .background(
                            if (isSelected) SurfaceElevated else SurfaceDark,
                            shape = SpaceXSmallShape
                        )
                        .clickable {
                            selectedDays = if (isSelected) {
                                selectedDays - dayNum
                            } else {
                                selectedDays + dayNum
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayLabel,
                        color = if (isSelected) com.example.checklist.theme.SpaceXAccent else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Presets: [平日 (月-金)] [週末 (土日)] [全日リセット]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SpaceXTextButton(
                text = "平日 (月-金)",
                onClick = { selectedDays = setOf(1, 2, 3, 4, 5) },
                prominent = selectedDays == setOf(1, 2, 3, 4, 5)
            )
            SpaceXTextButton(
                text = "週末 (土日)",
                onClick = { selectedDays = setOf(6, 7) },
                prominent = selectedDays == setOf(6, 7)
            )
            if (selectedDays.isNotEmpty()) {
                SpaceXTextButton(
                    text = "全日クリア",
                    onClick = { selectedDays = emptySet() },
                    prominent = false
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ITEMS (${items.size})",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpaceXTextButton(
                    text = "💡 プリセット提案",
                    onClick = { showPresetSuggestDialog = true },
                    prominent = false
                )
                SpaceXTextButton(
                    text = "+ 項目追加",
                    onClick = { showAddItemDialog = true },
                    prominent = true
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ITEMS LIST (ドラッグ＆ドロップ並び替え対応)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                val isReq = item.priority == Priority.REQUIRED
                val isDragging = item.id == draggingItemId

                Box(
                    modifier = (if (isDragging) {
                        Modifier
                            .fillMaxWidth()
                            .zIndex(15f)
                            .graphicsLayer {
                                translationY = dragOffsetY
                                scaleX = 1.03f
                                scaleY = 1.03f
                                shadowElevation = 15f
                            }
                            .border(2.dp, com.example.checklist.theme.SpaceXAccent, SpaceXShape)
                            .background(SurfaceElevated, shape = SpaceXShape)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (isReq) BorderProminent else BorderSubtle, SpaceXShape)
                            .background(if (isReq) SurfaceElevated else SurfaceDark, shape = SpaceXShape)
                    }).padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ドラッグハンドル
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .pointerInput(item.id) {
                                    detectTemplateDragAfterLongPress(
                                        onDragStart = {
                                            draggingItemId = item.id
                                            dragOffsetY = 0f
                                        },
                                        onDragDelta = { deltaY ->
                                            dragOffsetY += deltaY
                                            val currentIndex = items.indexOfFirst { it.id == item.id }
                                            val threshold = itemHeightPx * 0.6f
                                            if (currentIndex != -1) {
                                                if (dragOffsetY > threshold && currentIndex < items.size - 1) {
                                                    val temp = items[currentIndex]
                                                    items[currentIndex] = items[currentIndex + 1]
                                                    items[currentIndex + 1] = temp
                                                    dragOffsetY -= itemHeightPx
                                                } else if (dragOffsetY < -threshold && currentIndex > 0) {
                                                    val temp = items[currentIndex]
                                                    items[currentIndex] = items[currentIndex - 1]
                                                    items[currentIndex - 1] = temp
                                                    dragOffsetY += itemHeightPx
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggingItemId = null
                                            dragOffsetY = 0f
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "長押しして並び替え",
                                tint = if (isDragging) com.example.checklist.theme.SpaceXAccent else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (isReq) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = if (isReq) "重要 (CRIT)" else "標準 (STD)",
                                color = if (isReq) com.example.checklist.theme.SpaceXAccent else TextSecondary,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        IconButton(
                            onClick = { items.removeAt(index) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = TextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ADD ITEM DIALOG
    if (showAddItemDialog) {
        var title by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf(Priority.NORMAL) }

        Dialog(onDismissRequest = { showAddItemDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpaceBlack, shape = SpaceXShape)
                    .border(1.dp, BorderProminent, SpaceXShape)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "ADD TEMPLATE ITEM",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val isTitleValid = title.trim().isNotEmpty()
                    val submitItem = {
                        if (isTitleValid) {
                            items.add(TemplateItem(title = title.trim(), priority = priority))
                            showAddItemDialog = false
                        }
                    }

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
                                fontSize = 15.sp,
                                fontFamily = FontFamily.SansSerif
                            ),
                            cursorBrush = SolidColor(TextPrimary),
                            singleLine = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Priority selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (priority == Priority.REQUIRED) com.example.checklist.theme.SpaceXAccent else BorderSubtle,
                                    shape = SpaceXSmallShape
                                )
                                .background(
                                    if (priority == Priority.REQUIRED) SurfaceElevated else SpaceBlack,
                                    shape = SpaceXSmallShape
                                )
                                .clickable { priority = Priority.REQUIRED },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "重要 (CRIT)",
                                color = if (priority == Priority.REQUIRED) com.example.checklist.theme.SpaceXAccent else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (priority == Priority.NORMAL) TextPrimary else BorderSubtle,
                                    shape = SpaceXSmallShape
                                )
                                .background(
                                    if (priority == Priority.NORMAL) SurfaceElevated else SpaceBlack,
                                    shape = SpaceXSmallShape
                                )
                                .clickable { priority = Priority.NORMAL },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "標準 (STD)",
                                color = if (priority == Priority.NORMAL) TextPrimary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        SpaceXButton(
                            text = "CANCEL",
                            onClick = { showAddItemDialog = false }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SpaceXButton(
                            text = "ADD",
                            onClick = submitItem,
                            prominent = isTitleValid
                        )
                    }
                }
            }
        }
    }

    // PRESET SUGGESTION DIALOG
    if (showPresetSuggestDialog) {
        Dialog(onDismissRequest = { showPresetSuggestDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpaceBlack, shape = SpaceXShape)
                    .border(1.dp, BorderProminent, SpaceXShape)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "LOAD PRESET TEMPLATE",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "航空・宇宙運用の知見に基づく推奨タスク構成",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    presetTemplates.forEachIndexed { pIdx, preset ->
                        val isSelected = selectedPresetIndex == pIdx
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) com.example.checklist.theme.SpaceXAccent else BorderSubtle,
                                    shape = SpaceXShape
                                )
                                .background(if (isSelected) SurfaceElevated else SurfaceDark, shape = SpaceXShape)
                                .clickable { selectedPresetIndex = pIdx }
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = preset.name,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${preset.items.size} 項目 (重要 ${preset.items.count { it.priority == Priority.REQUIRED }}件)",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        SpaceXButton(
                            text = "CANCEL",
                            onClick = { showPresetSuggestDialog = false }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SpaceXButton(
                            text = "APPLY PRESET",
                            onClick = {
                                val selected = presetTemplates.getOrNull(selectedPresetIndex)
                                if (selected != null) {
                                    templateName = selected.name
                                    items.clear()
                                    items.addAll(selected.items)
                                }
                                showPresetSuggestDialog = false
                            },
                            prominent = true
                        )
                    }
                }
            }
        }
    }
}
