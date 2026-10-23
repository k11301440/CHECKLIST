package com.example.checklist.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp

import com.example.checklist.data.UiSize

// 右端まで自然に文字を詰めてから折り返す改行ルール (文節改行やBalancedによる途中の不自然な空白・早期改行を防止)
val SpaceXLineBreak = LineBreak(
    strategy = LineBreak.Strategy.Simple,
    strictness = LineBreak.Strictness.Loose,
    wordBreak = LineBreak.WordBreak.Default
)

// タスク名などで末尾1文字だけが次の段に落ちる現象（孤立文字）を防ぐユーティリティ
fun formatTaskText(text: String): String {
    val trimmed = text.trim()
    if (trimmed.length <= 2) return trimmed
    // 末尾から1文字手前がスペースの場合、ノーブレークスペースに置換
    val lastSpace = trimmed.lastIndexOf(' ')
    if (lastSpace != -1 && lastSpace == trimmed.length - 2) {
        return trimmed.substring(0, lastSpace) + "\u00A0" + trimmed.substring(lastSpace + 1)
    }
    return trimmed
}

fun getTypography(uiSize: UiSize = UiSize.MEDIUM): Typography {
    val (bodyLargeSp, bodyLargeLh) = when (uiSize) {
        UiSize.SMALL -> 15.sp to 22.sp
        UiSize.MEDIUM -> 16.5.sp to 24.sp
        UiSize.LARGE -> 18.sp to 26.sp
    }
    val (bodyMedSp, bodyMedLh) = when (uiSize) {
        UiSize.SMALL -> 13.5.sp to 19.sp
        UiSize.MEDIUM -> 14.5.sp to 21.sp
        UiSize.LARGE -> 16.sp to 23.sp
    }
    val (titleLargeSp, titleLargeLh) = when (uiSize) {
        UiSize.SMALL -> 20.sp to 26.sp
        UiSize.MEDIUM -> 22.sp to 28.sp
        UiSize.LARGE -> 24.sp to 30.sp
    }
    val (titleMedSp, titleMedLh) = when (uiSize) {
        UiSize.SMALL -> 17.sp to 22.sp
        UiSize.MEDIUM -> 18.5.sp to 24.sp
        UiSize.LARGE -> 20.5.sp to 26.sp
    }
    val (labelLargeSp, labelLargeLh) = when (uiSize) {
        UiSize.SMALL -> 12.sp to 16.sp
        UiSize.MEDIUM -> 13.5.sp to 18.sp
        UiSize.LARGE -> 15.sp to 20.sp
    }
    val (labelMedSp, labelMedLh) = when (uiSize) {
        UiSize.SMALL -> 10.5.sp to 15.sp
        UiSize.MEDIUM -> 11.5.sp to 16.sp
        UiSize.LARGE -> 13.sp to 18.sp
    }

    return Typography(
        displayLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = if (uiSize == UiSize.LARGE) 40.sp else if (uiSize == UiSize.SMALL) 34.sp else 38.sp,
            lineHeight = 44.sp,
            letterSpacing = 1.sp,
            lineBreak = SpaceXLineBreak
        ),
        displayMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = if (uiSize == UiSize.LARGE) 30.sp else if (uiSize == UiSize.SMALL) 26.sp else 28.sp,
            lineHeight = 34.sp,
            letterSpacing = 0.8.sp,
            lineBreak = SpaceXLineBreak
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = titleLargeSp,
            lineHeight = titleLargeLh,
            letterSpacing = 0.5.sp,
            lineBreak = SpaceXLineBreak
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = titleMedSp,
            lineHeight = titleMedLh,
            letterSpacing = 0.5.sp,
            lineBreak = SpaceXLineBreak
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = bodyLargeSp,
            lineHeight = bodyLargeLh,
            letterSpacing = 0.3.sp,
            lineBreak = SpaceXLineBreak
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = bodyMedSp,
            lineHeight = bodyMedLh,
            letterSpacing = 0.2.sp,
            lineBreak = SpaceXLineBreak
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = labelLargeSp,
            lineHeight = labelLargeLh,
            letterSpacing = 1.2.sp,
            lineBreak = SpaceXLineBreak
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = labelMedSp,
            lineHeight = labelMedLh,
            letterSpacing = 1.0.sp,
            lineBreak = SpaceXLineBreak
        )
    )
}

val Typography = getTypography(UiSize.MEDIUM)
