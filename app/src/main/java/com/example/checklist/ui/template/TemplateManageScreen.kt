package com.example.checklist.ui.template

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
                        items = items.toList()
                    )
                    viewModel.saveTemplate(finalTemplate)
                    onBack()
                },
                prominent = true
            )
        }

        SpaceXDivider()

        Spacer(modifier = Modifier.height(16.dp))

        // TEMPLATE NAME INPUT
        Text(
            text = "TEMPLATE NAME",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

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

        Spacer(modifier = Modifier.height(20.dp))

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

        // ITEMS LIST
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(items) { index, item ->
                val isReq = item.priority == Priority.REQUIRED

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isReq) BorderProminent else BorderSubtle, SpaceXShape)
                        .background(if (isReq) SurfaceElevated else SurfaceDark, shape = SpaceXShape)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isReq) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(18.dp)
                                    .background(TextPrimary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (isReq) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = if (isReq) "CRITICAL / REQUIRED" else "NORMAL",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // 上下並び替えボタン
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val temp = items[index]
                                        items[index] = items[index - 1]
                                        items[index - 1] = temp
                                    }
                                },
                                enabled = index > 0,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move Up",
                                    tint = if (index > 0) TextSecondary else BorderSubtle,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (index < items.size - 1) {
                                        val temp = items[index]
                                        items[index] = items[index + 1]
                                        items[index + 1] = temp
                                    }
                                },
                                enabled = index < items.size - 1,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move Down",
                                    tint = if (index < items.size - 1) TextSecondary else BorderSubtle,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

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
                            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                            cursorBrush = SolidColor(TextPrimary),
                            singleLine = true,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (title.isEmpty()) {
                            Text(text = "項目内容", color = TextTertiary, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isReq = priority == Priority.REQUIRED
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (isReq) TextPrimary else BorderSubtle, SpaceXSmallShape)
                                .background(if (isReq) SurfaceElevated else SpaceBlack, shape = SpaceXSmallShape)
                                .combinedClickable(onClick = { priority = Priority.REQUIRED })
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "必須",
                                color = if (isReq) TextPrimary else TextSecondary,
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
                                text = "普通",
                                color = if (isNorm) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isNorm) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        SpaceXTextButton(text = "CANCEL", onClick = { showAddItemDialog = false })
                        Spacer(modifier = Modifier.width(8.dp))
                        SpaceXButton(
                            text = "ADD",
                            onClick = {
                                if (title.isNotBlank()) {
                                    items.add(TemplateItem(title = title.trim(), priority = priority))
                                    showAddItemDialog = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // プリセット提案ダイアログ (ユーザーが許諾したらセットされる仕組み)
    if (showPresetSuggestDialog) {
        Dialog(onDismissRequest = { showPresetSuggestDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderProminent, SpaceXShape)
                    .background(SurfaceDark, shape = SpaceXShape)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "SUGGESTION // おすすめプリセット",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "業務フェーズに合わせた標準的なタスク構成を読み込みますか？選択して「セットする」を押すと項目が反映されます。",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    presetTemplates.forEachIndexed { pIdx, preset ->
                        val isSelected = pIdx == selectedPresetIndex
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) TextPrimary else BorderSubtle,
                                    shape = SpaceXSmallShape
                                )
                                .background(if (isSelected) SurfaceElevated else SpaceBlack, shape = SpaceXSmallShape)
                                .clickable { selectedPresetIndex = pIdx }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = preset.name,
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        text = "${preset.items.size}項目",
                                        color = TextTertiary,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                val previewText = preset.items.take(2).joinToString(" / ") { it.title }
                                Text(
                                    text = previewText,
                                    color = TextTertiary,
                                    fontSize = 9.5.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SpaceXButton(
                            text = "選択したプリセットを適用",
                            onClick = {
                                val chosen = presetTemplates.getOrNull(selectedPresetIndex)
                                if (chosen != null) {
                                    templateName = chosen.name
                                    items.clear()
                                    items.addAll(chosen.items)
                                }
                                showPresetSuggestDialog = false
                            },
                            prominent = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SpaceXTextButton(
                            text = "空のテンプレートで作成する",
                            onClick = { showPresetSuggestDialog = false }
                        )
                    }
                }
            }
        }
    }
}
