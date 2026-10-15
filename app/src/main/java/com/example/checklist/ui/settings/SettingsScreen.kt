package com.example.checklist.ui.settings

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
    modifier: Modifier = Modifier
) {
    var editingPhase by remember { mutableStateOf<Phase?>(null) }
    var showAddPhaseDialog by remember { mutableStateOf(false) }

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
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "SYSTEM CONFIG",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.size(36.dp))
        }

        SpaceXDivider()

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // SECTION 1: PHASES CONFIGURATION (MAX 3)
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PHASES (${state.phases.size}/6)",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        Row {
                            SpaceXTextButton(
                                text = "4分割に戻す",
                                onClick = { viewModel.restoreDefaultFourPhases() }
                            )
                            if (state.phases.size < 6) {
                                SpaceXTextButton(
                                    text = "+ 追加",
                                    onClick = { showAddPhaseDialog = true },
                                    prominent = true
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.phases.forEachIndexed { idx, phase ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                                    .background(SurfaceDark)
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "PHASE 0${idx + 1}",
                                            color = TextSecondary,
                                            fontSize = 10.sp,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = phase.name,
                                            color = TextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
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
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        if (state.phases.size > 1) {
                                            IconButton(
                                                onClick = { viewModel.removePhase(phase.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove phase",
                                                    tint = TextTertiary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: TEMPLATES MANAGEMENT
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TEMPLATES (${state.templates.size})",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        SpaceXTextButton(
                            text = "+ 新規テンプレート",
                            onClick = { onOpenTemplateEditor(null) },
                            prominent = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (state.templates.isEmpty()) {
                        Text(
                            text = "テンプレートはありません",
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.templates.forEach { tmpl ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                                        .background(SurfaceDark)
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tmpl.name,
                                                color = TextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${tmpl.items.size} 項目",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            SpaceXTextButton(
                                                text = "編集",
                                                onClick = { onOpenTemplateEditor(tmpl.id) }
                                            )

                                            IconButton(
                                                onClick = { viewModel.deleteTemplate(tmpl.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete template",
                                                    tint = TextTertiary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 3: REVIEW LOGS
            item {
                Column {
                    Text(
                        text = "DEVIATION / REVIEW LOGS (${state.reviewLogs.size})",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (state.reviewLogs.isEmpty()) {
                        Text(
                            text = "振り返りログはありません",
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    } else {
                        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.reviewLogs.take(10).forEach { log ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                                        .background(SurfaceDark)
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
                                                text = dateFormat.format(Date(log.timestamp)),
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        val statusLabel = if (log.hadIssue) "ISSUE REPORTED" else "NOMINAL (OK)"
                                        Text(
                                            text = statusLabel,
                                            color = if (log.hadIssue) TextPrimary else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = if (log.hadIssue) FontWeight.Bold else FontWeight.Normal,
                                            letterSpacing = 0.5.sp
                                        )

                                        if (!log.note.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "NOTE: ${log.note}",
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // EDIT PHASE NAME DIALOG
    if (editingPhase != null) {
        val phase = editingPhase!!
        var newName by remember { mutableStateOf(phase.name) }

        Dialog(onDismissRequest = { editingPhase = null }) {
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
                            value = newName,
                            onValueChange = { newName = it },
                            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                            cursorBrush = SolidColor(TextPrimary),
                            singleLine = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        SpaceXTextButton(text = "CANCEL", onClick = { editingPhase = null })
                        Spacer(modifier = Modifier.width(8.dp))
                        SpaceXButton(
                            text = "SAVE",
                            onClick = {
                                if (newName.isNotBlank()) {
                                    viewModel.updatePhaseName(phase.id, newName)
                                    editingPhase = null
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // ADD PHASE DIALOG
    if (showAddPhaseDialog) {
        var phaseName by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddPhaseDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpaceBlack)
                    .border(1.dp, BorderProminent, RoundedCornerShape(2.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "ADD PHASE (MAX 6)",
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
                            value = phaseName,
                            onValueChange = { phaseName = it },
                            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                            cursorBrush = SolidColor(TextPrimary),
                            singleLine = true,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (phaseName.isEmpty()) {
                            Text(
                                text = "フェーズ名 (例: 午後、夜間点検)",
                                color = TextTertiary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        SpaceXTextButton(text = "CANCEL", onClick = { showAddPhaseDialog = false })
                        Spacer(modifier = Modifier.width(8.dp))
                        SpaceXButton(
                            text = "ADD",
                            onClick = {
                                if (phaseName.isNotBlank()) {
                                    viewModel.addPhase(phaseName)
                                    showAddPhaseDialog = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
