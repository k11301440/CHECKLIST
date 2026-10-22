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
import com.example.checklist.ui.components.SpaceXButton
import com.example.checklist.ui.components.SpaceXDivider
import com.example.checklist.ui.components.SpaceXShape
import com.example.checklist.ui.components.SpaceXSmallShape
import com.example.checklist.ui.components.LocalLargeButtonMode
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

    val isEnglish = state.settings.appLanguage == com.example.checklist.data.AppLanguage.EN
    val isLarge = LocalLargeButtonMode.current

    val headerDateText = if (isActualToday) {
        if (isEnglish) "$todayFormatted • TODAY" else "$todayFormatted • 今日"
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
            val navButtonSize = if (isLarge) 42.dp else 36.dp
            val navIconSize = if (isLarge) 24.dp else 20.dp

            IconButton(
                onClick = onBack,
                modifier = Modifier.size(navButtonSize)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextSecondary,
                    modifier = Modifier.size(navIconSize)
                )
            }

            Row {
                IconButton(
                    onClick = onNavigateCalendar,
                    modifier = Modifier.size(navButtonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Calendar",
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
            }
        }

        SpaceXDivider()

        Spacer(modifier = Modifier.height(8.dp))

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
                    text = com.example.checklist.ui.common.AppStrings.backToToday(state.settings.appLanguage),
                    onClick = { viewModel.resetToToday() },
                    prominent = true
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = com.example.checklist.ui.common.AppStrings.dailyTimeline(state.settings.appLanguage),
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Text(
            text = com.example.checklist.ui.common.AppStrings.selectPhaseToMonitor(state.settings.appLanguage),
            color = TextSecondary,
            fontSize = 10.5.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // PHASE CARDS (4フェーズが1画面に収まるようコンパクトに配置)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(state.phases) { index, phase ->
                val phaseNumber = String.format("%02d", index + 1)
                val requiredTotal = phase.requiredPendingCount + phase.requiredDoneCount
                val requiredDone = phase.requiredDoneCount
                val normalTotal = phase.normalPendingCount + phase.normalDoneCount
                val normalDone = phase.normalDoneCount

                val isCompleted = phase.isCompleted
                val isSelected = index == state.selectedPhaseIndex

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) com.example.checklist.theme.SpaceXAccent else BorderProminent,
                            shape = SpaceXShape
                        )
                        .background(if (isSelected) SurfaceElevated else SurfaceDark, shape = SpaceXShape)
                        .combinedClickable(
                            onClick = {
                                viewModel.selectPhase(index)
                                onSelectPhase(index)
                            }
                        )
                        .padding(if (isLarge) 16.dp else 12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "PHASE $phaseNumber",
                                color = TextSecondary,
                                fontSize = if (isLarge) 12.sp else 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                            if (isCompleted) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Completed",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(if (isLarge) 15.dp else 13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = com.example.checklist.ui.common.AppStrings.done(state.settings.appLanguage),
                                        color = TextPrimary,
                                        fontSize = if (isLarge) 12.sp else 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            } else if (isSelected) {
                                Text(
                                    text = com.example.checklist.ui.common.AppStrings.active(state.settings.appLanguage),
                                    color = TextPrimary,
                                    fontSize = if (isLarge) 11.sp else 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier
                                        .border(1.dp, TextPrimary, SpaceXSmallShape)
                                        .padding(horizontal = if (isLarge) 7.dp else 5.dp, vertical = if (isLarge) 2.dp else 1.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = phase.name,
                            color = TextPrimary,
                            fontSize = if (isLarge) 22.sp else 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        SpaceXDivider()
                        Spacer(modifier = Modifier.height(6.dp))

                        // Counts breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isEnglish) "CRIT: " else "重要: ",
                                    color = TextSecondary,
                                    fontSize = if (isLarge) 12.5.sp else 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "$requiredDone / $requiredTotal",
                                    color = if (requiredTotal > 0 && requiredDone == requiredTotal) TextPrimary else TextSecondary,
                                    fontSize = if (isLarge) 13.5.sp else 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isEnglish) "STD: " else "標準: ",
                                    color = TextTertiary,
                                    fontSize = if (isLarge) 12.5.sp else 11.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "$normalDone / $normalTotal",
                                    color = TextTertiary,
                                    fontSize = if (isLarge) 13.5.sp else 12.sp,
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SpaceXButton(
                        text = com.example.checklist.ui.common.AppStrings.advanceToNextDay(state.settings.appLanguage),
                        onClick = { viewModel.advanceToNextDay() },
                        prominent = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SpaceXButton(
                        text = com.example.checklist.ui.common.AppStrings.resetAllPhases(state.settings.appLanguage),
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
            title = com.example.checklist.ui.common.AppStrings.resetAllPhasesTitle(state.settings.appLanguage),
            message = com.example.checklist.ui.common.AppStrings.resetAllPhasesMessage(state.settings.appLanguage),
            confirmText = com.example.checklist.ui.common.AppStrings.resetAll(state.settings.appLanguage),
            cancelText = com.example.checklist.ui.common.AppStrings.cancel(state.settings.appLanguage),
            onConfirm = {
                viewModel.resetAllPhases()
                showResetAllDialog = false
            },
            onDismiss = { showResetAllDialog = false }
        )
    }
}
