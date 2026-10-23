package com.example.checklist.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.checklist.data.UiSize
import com.example.checklist.theme.BorderProminent
import com.example.checklist.theme.BorderSubtle
import com.example.checklist.theme.SpaceBlack
import com.example.checklist.theme.TextPrimary
import com.example.checklist.theme.TextSecondary
import com.example.checklist.theme.TextTertiary

val SpaceXCornerRadius = 8.dp
val SpaceXShape = RoundedCornerShape(SpaceXCornerRadius)
val SpaceXDialogShape = RoundedCornerShape(10.dp)
val SpaceXSmallShape = RoundedCornerShape(4.dp)

val LocalUiSize = androidx.compose.runtime.compositionLocalOf { UiSize.MEDIUM }
val LocalLargeButtonMode = androidx.compose.runtime.compositionLocalOf { false }

@Composable
fun uiDimension(small: Dp, medium: Dp, large: Dp): Dp {
    return when (LocalUiSize.current) {
        UiSize.SMALL -> small
        UiSize.MEDIUM -> medium
        UiSize.LARGE -> large
    }
}

@Composable
fun uiFontSize(small: TextUnit, medium: TextUnit, large: TextUnit): TextUnit {
    return when (LocalUiSize.current) {
        UiSize.SMALL -> small
        UiSize.MEDIUM -> medium
        UiSize.LARGE -> large
    }
}

/**
 * 端末のフォントスケール設定（fontScale）に連動して Dp をスケーリングする拡張関数。
 * 端末設定で文字サイズを拡大した際、アイコンや関連タッチ領域も自動的に追従して拡大される。
 *
 * @param minScale 最小スケール（1.0未満には縮小しない）
 * @param maxScale 最大スケール（過剰な崩れを防ぐため上限を設ける。デフォルト1.8倍）
 */
@Composable
fun Dp.scaledWithFont(minScale: Float = 1.0f, maxScale: Float = 1.8f): Dp {
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceIn(minScale, maxScale)
    return this * fontScale
}

// SpaceX Minimal Outline Button
@Composable
fun SpaceXButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    prominent: Boolean = true,
    inverted: Boolean = false,
    leadingIcon: ImageVector? = null,
    shape: Shape = SpaceXShape
) {
    val uiSize = LocalUiSize.current
    val borderColor = if (enabled) {
        if (inverted) TextPrimary else if (prominent) TextPrimary else BorderProminent
    } else BorderSubtle
    val textColor = if (enabled) {
        if (inverted) SpaceBlack else if (prominent) TextPrimary else TextSecondary
    } else TextTertiary
    val containerBg = if (enabled && inverted) TextPrimary else SpaceBlack

    val buttonHeight = when (uiSize) {
        UiSize.SMALL -> 44.dp
        UiSize.MEDIUM -> 48.dp
        UiSize.LARGE -> 56.dp
    }
    val fontSize = when (uiSize) {
        UiSize.SMALL -> 12.sp
        UiSize.MEDIUM -> 13.5.sp
        UiSize.LARGE -> 15.sp
    }
    val contentPadding = when (uiSize) {
        UiSize.SMALL -> PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        UiSize.MEDIUM -> PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        UiSize.LARGE -> PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    }
    val iconSize = when (uiSize) {
        UiSize.SMALL -> 15.dp
        UiSize.MEDIUM -> 17.dp
        UiSize.LARGE -> 19.dp
    }

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(buttonHeight),
        shape = shape,
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerBg,
            contentColor = textColor,
            disabledContentColor = TextTertiary
        ),
        contentPadding = contentPadding
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = textColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )
        }
    }
}

// SpaceX Minimal Text Button
@Composable
fun SpaceXTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    shape: Shape = SpaceXShape
) {
    val uiSize = LocalUiSize.current
    val minHeight = when (uiSize) {
        UiSize.SMALL -> 32.dp
        UiSize.MEDIUM -> 38.dp
        UiSize.LARGE -> 44.dp
    }
    val fontSize = when (uiSize) {
        UiSize.SMALL -> 11.5.sp
        UiSize.MEDIUM -> 12.5.sp
        UiSize.LARGE -> 14.sp
    }
    val contentPadding = when (uiSize) {
        UiSize.SMALL -> PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        UiSize.MEDIUM -> PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        UiSize.LARGE -> PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    }

    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = minHeight),
        shape = shape,
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (prominent) TextPrimary else TextSecondary
        ),
        contentPadding = contentPadding
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.0.sp
        )
    }
}

// SpaceX Minimal Wireframe Checkbox (文字サイズ連動)
@Composable
fun SpaceXCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    val uiSize = LocalUiSize.current
    val baseSize = when (uiSize) {
        UiSize.SMALL -> if (size == 20.dp) 20.dp else size
        UiSize.MEDIUM -> if (size == 20.dp) 24.dp else size
        UiSize.LARGE -> if (size == 20.dp) 28.dp else size
    }
    val effectiveSize = baseSize.scaledWithFont()
    Box(
        modifier = modifier
            .size(effectiveSize)
            .border(
                width = 1.dp,
                color = if (checked) TextSecondary else TextPrimary,
                shape = SpaceXSmallShape
            )
            .background(SpaceBlack),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Checked",
                modifier = Modifier.size((effectiveSize - 4.dp).coerceAtLeast(12.dp)),
                tint = TextPrimary
            )
        }
    }
}

// Section Divider with subtle border
@Composable
fun SpaceXDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = BorderSubtle
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}

// SpaceX Minimal Top Bar
@Composable
fun SpaceXTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (navigationIcon != null) {
                    navigationIcon()
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }
            if (actions != null) {
                actions()
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }
        }
        SpaceXDivider()
    }
}

// Minimal Confirmation Dialog for irreversible operations
@Composable
fun SpaceXConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "PROCEED",
    cancelText: String = "CANCEL",
    isDanger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceBlack, shape = SpaceXDialogShape)
                .border(1.dp, if (isDanger) Color(0xFFFF453A) else BorderProminent, SpaceXDialogShape)
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = title.uppercase(),
                    color = if (isDanger) Color(0xFFFF453A) else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = message,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpaceXTextButton(
                        text = cancelText,
                        onClick = onDismiss
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    if (isDanger) {
                        SpaceXDangerButton(
                            text = confirmText,
                            onClick = onConfirm
                        )
                    } else {
                        SpaceXButton(
                            text = confirmText,
                            onClick = onConfirm,
                            prominent = true
                        )
                    }
                }
            }
        }
    }
}

// SpaceX Minimal Danger Button (初期化や破壊的操作用の赤色アクセント)
@Composable
fun SpaceXDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = SpaceXShape
) {
    val uiSize = LocalUiSize.current
    val dangerColor = Color(0xFFFF453A)
    val buttonHeight = when (uiSize) {
        UiSize.SMALL -> 44.dp
        UiSize.MEDIUM -> 48.dp
        UiSize.LARGE -> 56.dp
    }
    val fontSize = when (uiSize) {
        UiSize.SMALL -> 12.sp
        UiSize.MEDIUM -> 13.5.sp
        UiSize.LARGE -> 15.sp
    }
    val contentPadding = when (uiSize) {
        UiSize.SMALL -> PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        UiSize.MEDIUM -> PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        UiSize.LARGE -> PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(buttonHeight),
        shape = shape,
        border = BorderStroke(1.dp, dangerColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SpaceBlack,
            contentColor = dangerColor
        ),
        contentPadding = contentPadding
    ) {
        Text(
            text = text,
            color = dangerColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}

