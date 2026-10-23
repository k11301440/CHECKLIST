package com.example.checklist.ui.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.checklist.data.*
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
import com.example.checklist.ui.components.SpaceXShape
import com.example.checklist.ui.components.SpaceXSmallShape
import com.example.checklist.ui.components.SpaceXTopBar
import com.example.checklist.ui.components.LocalLargeButtonMode
import com.example.checklist.ui.components.uiDimension
import com.example.checklist.ui.components.uiFontSize
import com.example.checklist.ui.components.scaledWithFont
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    state: AppState,
    viewModel: ChecklistViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(today) }

    val selectedDateString = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    val dailyRecord = state.dailyRecords[selectedDateString]

    val canGoPrevMonth = currentYearMonth > YearMonth.now()
    var swipeCalendarDragX by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = with(LocalDensity.current) { 60.dp.toPx() }

    val swipeCalendarModifier = Modifier.pointerInput(currentYearMonth, canGoPrevMonth) {
        detectHorizontalDragGestures(
            onDragEnd = {
                if (swipeCalendarDragX < -swipeThresholdPx) {
                    // Swiped Left -> Next Month
                    currentYearMonth = currentYearMonth.plusMonths(1)
                } else if (swipeCalendarDragX > swipeThresholdPx) {
                    // Swiped Right -> Prev Month (今月以前には戻れない)
                    if (canGoPrevMonth) {
                        currentYearMonth = currentYearMonth.minusMonths(1)
                    }
                }
                swipeCalendarDragX = 0f
            },
            onDragCancel = {
                swipeCalendarDragX = 0f
            },
            onHorizontalDrag = { _, dragAmount ->
                swipeCalendarDragX += dragAmount
            }
        )
    }

    // 今日の振り返りログから該当日のログを抽出
    val matchingLogs = state.reviewLogs.filter { log ->
        val logDate = log.dateString ?: run {
            val d = java.time.Instant.ofEpochMilli(log.timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            d.format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
        logDate == selectedDateString
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        // Top Bar
        SpaceXTopBar(
            title = com.example.checklist.ui.common.AppStrings.calendar(state.settings.appLanguage),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
            },
            actions = {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .border(1.dp, TextPrimary, SpaceXSmallShape)
                        .clickable {
                            selectedDate = today
                            currentYearMonth = YearMonth.from(today)
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = com.example.checklist.ui.common.AppStrings.today(state.settings.appLanguage),
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .then(swipeCalendarModifier)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Year / Month Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${currentYearMonth.year}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)).uppercase(),
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    Row {
                        val monthBtnSize = uiDimension(34.dp, 38.dp, 44.dp).scaledWithFont()
                        val monthIconSize = uiDimension(18.dp, 21.dp, 24.dp).scaledWithFont()
                        val canGoPrev = currentYearMonth > YearMonth.now()
                        IconButton(
                            onClick = { if (canGoPrev) currentYearMonth = currentYearMonth.minusMonths(1) },
                            enabled = canGoPrev,
                            modifier = Modifier.size(monthBtnSize)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Prev Month",
                                tint = if (canGoPrev) TextPrimary else TextDisabled,
                                modifier = Modifier.size(monthIconSize)
                            )
                        }
                        IconButton(
                            onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                            modifier = Modifier.size(monthBtnSize)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Month",
                                tint = TextPrimary,
                                modifier = Modifier.size(monthIconSize)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Weekday Headers (月曜始まり: 最左列が月曜、最右列が日曜)
                val dayOfWeeks = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                Row(modifier = Modifier.fillMaxWidth()) {
                    for ((dIdx, dayName) in dayOfWeeks.withIndex()) {
                        val headerColor = when (dIdx) {
                            5 -> androidx.compose.ui.graphics.Color(0xFF6B9CE8) // SAT: 控えめな落ち着いた青
                            6 -> androidx.compose.ui.graphics.Color(0xFFE57373) // SUN: 控えめな落ち着いた赤
                            else -> TextTertiary
                        }
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayName,
                                color = headerColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Month Calendar Grid (月曜始まり)
                val firstDayOfMonth = currentYearMonth.atDay(1)
                val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value - 1 // 0 for Monday, 6 for Sunday
                val daysInMonth = currentYearMonth.lengthOfMonth()

                val totalCells = ((dayOfWeekOffset + daysInMonth + 6) / 7) * 7
                val weeks = totalCells / 7

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, SpaceXShape)
                        .padding(4.dp)
                ) {
                    for (weekIndex in 0 until weeks) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (dayCol in 0 until 7) {
                                val cellIndex = weekIndex * 7 + dayCol
                                val dayNumber = cellIndex - dayOfWeekOffset + 1
                                val isValidDay = dayNumber in 1..daysInMonth

                                if (isValidDay) {
                                    val date = currentYearMonth.atDay(dayNumber)
                                    val isSelected = date == selectedDate
                                    val isToday = date == today
                                    val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                    val hasRecord = state.dailyRecords.containsKey(dateStr)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .border(
                                                width = when {
                                                    isSelected -> 2.dp
                                                    isToday -> 1.dp
                                                    else -> 0.dp
                                                },
                                                color = when {
                                                    isSelected -> com.example.checklist.theme.SpaceXAccent
                                                    isToday -> TextSecondary
                                                    else -> SpaceBlack
                                                },
                                                shape = SpaceXSmallShape
                                            )
                                            .background(
                                                when {
                                                    isSelected -> SurfaceElevated
                                                    hasRecord -> SurfaceDark
                                                    else -> SpaceBlack
                                                }
                                            )
                                            .combinedClickable(
                                                onClick = { selectedDate = date },
                                                onDoubleClick = {
                                                    if (date == today) {
                                                        viewModel.returnToCurrentPhase()
                                                        onBack()
                                                    } else {
                                                        selectedDate = date
                                                    }
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "$dayNumber",
                                                color = when {
                                                    isSelected -> TextPrimary
                                                    isToday -> TextPrimary
                                                    dayCol == 5 -> androidx.compose.ui.graphics.Color(0xFF7FAEFF) // SAT
                                                    dayCol == 6 -> androidx.compose.ui.graphics.Color(0xFFFF8585) // SUN
                                                    else -> TextPrimary // 月曜含む平日はクリアに表示
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (hasRecord) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(3.dp)
                                                        .background(TextPrimary)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Selected Date Detail Section (一覧表)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = com.example.checklist.ui.common.AppStrings.dateLog(state.settings.appLanguage),
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd EEEE", Locale.ENGLISH)).uppercase(),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    if (selectedDate == today) {
                        Text(
                            text = com.example.checklist.ui.common.AppStrings.currentSol(state.settings.appLanguage),
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .border(1.dp, BorderProminent, SpaceXSmallShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Summary Table
            if (dailyRecord != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderProminent, SpaceXShape)
                            .background(SurfaceDark, shape = SpaceXShape)
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        com.example.checklist.ui.common.AppStrings.completedItems(state.settings.appLanguage),
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        "${dailyRecord.completedItems} / ${dailyRecord.totalItems}",
                                        color = TextPrimary,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        com.example.checklist.ui.common.AppStrings.reqPending(state.settings.appLanguage),
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        "${dailyRecord.requiredPendingItems}",
                                        color = if (dailyRecord.requiredPendingItems > 0) TextPrimary else TextSecondary,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                com.example.checklist.ui.common.AppStrings.phaseBreakdown(state.settings.appLanguage),
                                color = TextSecondary,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            for (phaseSummary in dailyRecord.phaseSummaries) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(if (phaseSummary.isCompleted) TextPrimary else TextTertiary)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            phaseSummary.phaseName,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        "${phaseSummary.completedCount} / ${phaseSummary.totalCount} 完了",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                if (phaseSummary.reviewNote != null) {
                                    Text(
                                        text = "メモ: ${phaseSummary.reviewNote}",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 4.dp)
                                    )
                                }
                            }

                            // 日別スナップショット（過去のタスクとチェック状態の一覧詳細）
                            if (dailyRecord.phasesSnapshot.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (state.settings.appLanguage == AppLanguage.EN) "TASK ARCHIVE (COMPLETION STATUS)" else "タスク記録（チェック実績）",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                for (snapPhase in dailyRecord.phasesSnapshot) {
                                    val validItems = snapPhase.items.filterNot { it.isDeleted }
                                    if (validItems.isNotEmpty()) {
                                        Text(
                                            text = snapPhase.name,
                                            color = com.example.checklist.theme.SpaceXAccent,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                        )
                                        for (item in validItems) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (item.isCompleted) com.example.checklist.theme.SpaceXAccent else TextDisabled,
                                                            shape = SpaceXSmallShape
                                                        )
                                                        .background(
                                                            color = if (item.isCompleted) com.example.checklist.theme.SpaceXAccent else androidx.compose.ui.graphics.Color.Transparent,
                                                            shape = SpaceXSmallShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (item.isCompleted) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Done",
                                                            tint = SpaceBlack,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = item.title,
                                                    color = if (item.isCompleted) TextPrimary else TextSecondary,
                                                    fontSize = 12.5.sp,
                                                    textDecoration = if (item.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else if (selectedDate == today) {
                // 本日のライブステータス（今日の場合はリアルタイムのフェーズ状況を表示）
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSubtle, SpaceXShape)
                            .background(SurfaceDark, shape = SpaceXShape)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                com.example.checklist.ui.common.AppStrings.todaysLiveStatus(state.settings.appLanguage),
                                color = TextSecondary,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            val liveRecord = TaskSync.createDailyRecord(selectedDateString, state.phases)
                            val totalItems = liveRecord.totalItems
                            val completedItems = liveRecord.completedItems
                            val reqPending = liveRecord.requiredPendingItems

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        com.example.checklist.ui.common.AppStrings.completedItems(state.settings.appLanguage),
                                        color = TextTertiary,
                                        fontSize = 10.sp
                                    )
                                    Text("$completedItems / $totalItems", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        com.example.checklist.ui.common.AppStrings.reqPending(state.settings.appLanguage),
                                        color = TextTertiary,
                                        fontSize = 10.sp
                                    )
                                    Text("$reqPending", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            for (phase in state.phases) {
                                val doneCount = phase.totalDoneCount
                                val validTotal = phase.totalValidCount
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(if (phase.isCompleted) TextPrimary else TextTertiary)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(phase.name, color = TextPrimary, fontSize = 13.sp)
                                    }
                                    Text(
                                        "$doneCount / $validTotal 完了",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSubtle, SpaceXShape)
                            .background(SurfaceDark, shape = SpaceXShape)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = com.example.checklist.ui.common.AppStrings.noMissionLog(state.settings.appLanguage),
                            color = TextTertiary,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // この日付の振り返りログ一覧
            if (matchingLogs.isNotEmpty()) {
                item {
                    Text(
                        text = com.example.checklist.ui.common.AppStrings.reviewLogs(state.settings.appLanguage, matchingLogs.size),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(matchingLogs) { log ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSubtle, SpaceXShape)
                            .background(SurfaceDark, shape = SpaceXShape)
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = log.phaseName,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (log.hadIssue) com.example.checklist.ui.common.AppStrings.issueNoted(state.settings.appLanguage) else com.example.checklist.ui.common.AppStrings.allClear(state.settings.appLanguage),
                                    color = if (log.hadIssue) TextPrimary else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (log.note != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = log.note,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
