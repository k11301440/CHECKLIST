package com.example.checklist.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.checklist.theme.BorderProminent
import com.example.checklist.theme.BorderSubtle
import com.example.checklist.theme.SpaceBlack
import com.example.checklist.theme.SurfaceDark
import com.example.checklist.theme.SurfaceElevated
import com.example.checklist.theme.TextDisabled
import com.example.checklist.theme.TextPrimary
import com.example.checklist.theme.TextSecondary
import com.example.checklist.theme.TextTertiary
import com.example.checklist.ui.components.SpaceXDivider
import com.example.checklist.ui.components.SpaceXTopBar

@Composable
fun UserGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        SpaceXTopBar(
            title = "FLIGHT MANUAL // 使い方ガイド",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
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

                Text(
                    text = "MISSION EXECUTION GUIDE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "運用マニュアル & チュートリアル",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                GuideSection(
                    stepNumber = "01",
                    title = "タスク種別: REQ と NORM",
                    description = "・[REQ] 必須タスク: そのフェーズで絶対に完遂すべき最優先事項です。未完了のままフェーズが進むと、設定に応じて次フェーズや翌日へ自動引き継ぎ（キャリーオーバー）されます。\n・[NORM] 通常タスク: 日常的なルーチンや補足タスクです。"
                )

                GuideSection(
                    stepNumber = "02",
                    title = "自由なドラッグ＆ドロップ並び替え",
                    description = "タスクカード左端を長押しするとカードが浮き上がり、上下にドラッグすることで並び順を瞬時に入れ替えられます。優先度の変化に合わせて、REQとNORMの境界を越えて自由に並び替えが可能です。"
                )

                GuideSection(
                    stepNumber = "03",
                    title = "クイック追加 & 未来タスク予約",
                    description = "タスク画面下部の入力バーから、いつでも単発タスクを即時追加できます。日付アイコン横のオプションを開くと、タスクを開始する日付（今日・明日・明後日）やフェーズを指定して事前登録できます。"
                )

                GuideSection(
                    stepNumber = "04",
                    title = "振り返りログ (ALL SYSTEMS NOMINAL?)",
                    description = "フェーズ進行時、問題がなかったか（NOMINAL）の振り返りが行えます。「NO」を選択すると課題やブロッカーをメモとして記録でき、カレンダー画面でいつでも過去のミッション記録を振り返ることができます。"
                )

                GuideSection(
                    stepNumber = "05",
                    title = "テンプレート編集と未来への即時同期",
                    description = "設定画面の「TEMPLATES」から各フェーズの基本タスク構成を自由に編集できます。テンプレートを変更すると、現在いるフェーズ以降および翌日以降のタスク構成へ直ちに反映されます。"
                )

                GuideSection(
                    stepNumber = "06",
                    title = "未完了REQタスクの通知バー常駐",
                    description = "設定画面で「未完了REQタスクの通知」をオン（オプトイン）にすると、現在フェーズの未処理必須タスクがAndroid通知バーに常時表示され、重要な仕事の見落としを防止します。"
                )

                GuideSection(
                    stepNumber = "07",
                    title = "完全ローカル・通信ゼロの安全性",
                    description = "すべてのデータはお使いの端末内ローカルストレージにのみ保存され、外部サーバーへの通信は一切発生しません。飛行機内やオフライン環境でも100%確実に動作します。"
                )

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun GuideSection(
    stepNumber: String,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, BorderSubtle, com.example.checklist.ui.components.SpaceXShape)
            .background(SurfaceDark, shape = com.example.checklist.ui.components.SpaceXShape)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stepNumber,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .border(1.dp, BorderProminent, com.example.checklist.ui.components.SpaceXSmallShape)
                        .background(SurfaceElevated, shape = com.example.checklist.ui.components.SpaceXSmallShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.3.sp
            )
        }
    }
}
