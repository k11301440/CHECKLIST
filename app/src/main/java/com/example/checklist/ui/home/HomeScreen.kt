package com.example.checklist.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.checklist.data.AppState
import com.example.checklist.data.Priority
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: AppState,
    viewModel: ChecklistViewModel,
    onSelectPhase: (Int) -> Unit,
    onNavigateSettings: () -> Unit,
    onBack: () -> Unit,
    onNavigateCalendar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showResetAllDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val isActualToday = remember(state.currentDate) {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        state.currentDate.isEmpty() || state.currentDate == todayStr
    }

    val todayFormatted = remember(state.currentDate) {
        runCatching {
            LocalDate.parse(state.currentDate).format(DateTimeFormatter.ofPattern("yyyy.MM.dd EEEE", Locale.ENGLISH)).uppercase()
        }.getOrElse {
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd EEEE", Locale.ENGLISH)).uppercase()
        }
    }

    val headerDateText = if (isActualToday) {
        "$todayFormatted // TODAY"
    } else {
        todayFormatted
    }

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
                text = "FLIGHT PHASES",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Row {
                IconButton(
                    onClick = onNavigateCalendar,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Calendar",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onNavigateSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        SpaceXDivider()

        Spacer(modifier = Modifier.height(20.dp))

        // Date Display & Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = headerDateText,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            if (!isActualToday) {
                com.example.checklist.ui.components.SpaceXTextButton(
                    text = "TODAYに戻る",
                    onClick = { viewModel.resetToToday() },
                    prominent = true
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "DAILY TIMELINE",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Text(
            text = "SELECT PHASE TO MONITOR & EXECUTE",
            color = TextSecondary,
            fontSize = 11.sp,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // PHASE CARDS
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(state.phases) { index, phase ->
                val phaseNumber = String.format("%02d", index + 1)
                val requiredTotal = phase.items.count { it.priority == Priority.REQUIRED }
                val requiredDone = phase.items.count { it.priority == Priority.REQUIRED && it.isCompleted }
                val normalTotal = phase.items.count { it.priority == Priority.NORMAL }
                val normalDone = phase.items.count { it.priority == Priority.NORMAL && it.isCompleted }

                val isCompleted = phase.isCompleted
                val isSelected = index == state.selectedPhaseIndex

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) TextPrimary else BorderProminent,
                            shape = RoundedCornerShape(2.dp)
                        )
                        .background(if (isSelected) SurfaceElevated else SurfaceDark)
                        .combinedClickable(
                            onClick = {
                                viewModel.selectPhase(index)
                                onSelectPhase(index)
                            }
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PHASE $phaseNumber",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            if (isCompleted) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Completed",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "DONE",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            } else if (isSelected) {
                                Text(
                                    text = "ACTIVE",
                                    color = TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier
                                        .border(1.dp, TextPrimary, RoundedCornerShape(2.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = phase.name,
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        SpaceXDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        // Counts breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "REQ: ",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "$requiredDone / $requiredTotal",
                                    color = if (requiredTotal > 0 && requiredDone == requiredTotal) TextPrimary else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "NORM: ",
                                    color = TextTertiary,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "$normalDone / $normalTotal",
                                    color = TextTertiary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }

                        if (phase.reviewNote != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "NOTE: ${phase.reviewNote}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SpaceXButton(
                        text = "ADVANCE TO NEXT DAY",
                        onClick = { viewModel.advanceToNextDay() },
                        prominent = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SpaceXButton(
                        text = "RESET ALL PHASES",
                        onClick = { showResetAllDialog = true },
                        prominent = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showResetAllDialog) {
        com.example.checklist.ui.components.SpaceXConfirmDialog(
            title = "CONFIRM RESET // ALL PHASES",
            message = "すべてのフェーズのタスク完了状況をリセットしますか？この日のログは保持されます。",
            confirmText = "RESET ALL",
            onConfirm = {
                viewModel.resetAllPhases()
                showResetAllDialog = false
            },
            onDismiss = { showResetAllDialog = false }
        )
    }
}
