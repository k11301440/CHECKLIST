package com.example.checklist.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.checklist.data.AppState
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

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
            title = "CALENDAR",
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
                        text = "TODAY",
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
                        IconButton(
                            onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Prev Month",
                                tint = TextPrimary
                            )
                        }
                        IconButton(
                            onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Month",
                                tint = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Weekday Headers (月曜始まり: 最左列が月曜、最右列が日曜)
                val dayOfWeeks = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (dayName in dayOfWeeks) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayName,
                                color = TextTertiary,
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
                                                    isSelected -> TextPrimary
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
                                            .clickable { selectedDate = date },
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
                                                    dayCol == 0 || dayCol == 6 -> TextSecondary
                                                    else -> TextPrimary
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
                            text = "DATE LOG // 一覧表",
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
                            text = "CURRENT SOL",
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
                                    Text("COMPLETED ITEMS", color = TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp)
                                    Text(
                                        "${dailyRecord.completedItems} / ${dailyRecord.totalItems}",
                                        color = TextPrimary,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("REQ PENDING", color = TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp)
                                    Text(
                                        "${dailyRecord.requiredPendingItems}",
                                        color = if (dailyRecord.requiredPendingItems > 0) TextPrimary else TextSecondary,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text("PHASE BREAKDOWN", color = TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp)
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
                            Text("TODAY'S LIVE STATUS", color = TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(10.dp))

                            val totalItems = state.phases.sumOf { it.items.size }
                            val completedItems = state.phases.sumOf { p -> p.items.count { it.isCompleted } }
                            val reqPending = state.phases.sumOf { p -> p.items.count { it.priority == com.example.checklist.data.Priority.REQUIRED && !it.isCompleted } }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("COMPLETED ITEMS", color = TextTertiary, fontSize = 10.sp)
                                    Text("$completedItems / $totalItems", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("REQ PENDING", color = TextTertiary, fontSize = 10.sp)
                                    Text("$reqPending", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            for (phase in state.phases) {
                                val doneCount = phase.items.count { it.isCompleted }
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
                                        "$doneCount / ${phase.items.size} 完了",
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
                            text = "NO MISSION LOG RECORDED",
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
                        text = "REVIEW LOGS (${matchingLogs.size})",
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
                                    text = if (log.hadIssue) "ISSUE NOTED" else "ALL CLEAR",
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
