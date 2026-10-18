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
    state: com.example.checklist.data.AppState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnglish = state.settings.appLanguage == com.example.checklist.data.AppLanguage.EN

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        SpaceXTopBar(
            title = if (isEnglish) "TUTORIAL" else "チュートリアル",
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
                    title = if (isEnglish) "Task Types: Critical & Standard" else "タスク種別: 重要 と 標準",
                    description = if (isEnglish) 
                        "• [CRITICAL] Vital tasks that must be executed in this phase. Uncompleted critical tasks automatically carry forward to next phases.\n• [STANDARD] Routine or supplementary tasks."
                    else 
                        "・[重要] そのフェーズで絶対に完遂すべき最優先事項です。未完了のままフェーズが進むと、設定に応じて次フェーズや翌日へ自動引き継ぎ（キャリーオーバー）されます。\n・[標準] 日常的なルーチンや補足タスクです。"
                )

                GuideSection(
                    stepNumber = "02",
                    title = if (isEnglish) "Drag & Drop Reordering" else "自由なドラッグ＆ドロップ並び替え",
                    description = if (isEnglish)
                        "Long-press the handle on the left of any task card to float it, then drag up or down to reorder effortlessly across priority sections."
                    else
                        "タスクカード左端を長押しするとカードが浮き上がり、上下にドラッグすることで並び順を瞬時に入れ替えられます。重要と標準の枠を越えて自由に並び替えが可能です。"
                )

                GuideSection(
                    stepNumber = "03",
                    title = if (isEnglish) "Horizontal Swipe Phase Navigation" else "画面横スワイプでのフェーズ移動",
                    description = if (isEnglish)
                        "Swipe left or right anywhere on the task screen to seamlessly switch between phases. Moving past the first or last phase triggers a confirmation dialog before crossing days. This gesture can be toggled in Settings."
                    else
                        "タスク画面を左右にスワイプすることで、素早く前後のフェーズへ切り替えられます。最終フェーズから翌日に進む際や、前日に戻る際には日跨ぎ確認ポップアップが表示されます。この機能は設定画面でいつでもON/OFFを切り替え可能です。"
                )

                GuideSection(
                    stepNumber = "04",
                    title = if (isEnglish) "Quick Add & Future Scheduling" else "クイック追加 & 未来タスク予約",
                    description = if (isEnglish)
                        "Add ad-hoc tasks anytime from the bottom input bar. Expand options to select starting date (today, tomorrow, in 2 days) and phase."
                    else
                        "タスク画面下部の入力バーから、いつでも単発タスクを即時追加できます。日付アイコン横のオプションを開くと、タスクを開始する日付（今日・明日・明後日）やフェーズを指定して事前登録できます。"
                )

                GuideSection(
                    stepNumber = "05",
                    title = if (isEnglish) "Review Log & Calendar Jump" else "振り返りログとカレンダー日付ジャンプ",
                    description = if (isEnglish)
                        "When advancing phases, verify whether execution was nominal. Selecting NO allows recording blockers into the mission calendar log. Double-tapping any date in the calendar instantly jumps to that date's task cockpit."
                    else
                        "フェーズ進行時、問題がなかったか（NOMINAL）の振り返りが行えます。「NO」を選択すると課題やブロッカーをメモとして記録でき、カレンダー画面で過去のミッション記録を確認できます。カレンダーの日付をダブルタップすると、その日付のタスク画面へ直接ジャンプできます（当月以前への無用な逆戻りは防止されています）。"
                )

                GuideSection(
                    stepNumber = "06",
                    title = if (isEnglish) "Templates & Future Sync" else "テンプレート編集と未来への即時同期",
                    description = if (isEnglish)
                        "Customize recurring phase tasks from Settings > Templates. Changes immediately synchronize with future phases and upcoming days."
                    else
                        "設定画面の「TEMPLATES」から各フェーズの基本タスク構成を自由に編集できます。テンプレートを変更すると、現在いるフェーズ以降および翌日以降のタスク構成へ直ちに反映されます。"
                )

                GuideSection(
                    stepNumber = "07",
                    title = if (isEnglish) "Status Bar Notifications" else "未完了タスクの通知バー常駐",
                    description = if (isEnglish)
                        "Enable 'Critical task notifications' to keep remaining priority tasks visible in Android's notification tray."
                    else
                        "設定画面で「未完了の重要タスクを常駐通知」をオンにすると、現在フェーズの未処理重要タスクがAndroid通知バーに常時表示され、重要な仕事の見落としを防止します。"
                )

                GuideSection(
                    stepNumber = "08",
                    title = if (isEnglish) "Offline-First Local Storage" else "完全ローカル・通信ゼロの安全性",
                    description = if (isEnglish)
                        "All data stays safely stored in local device JSON with zero network transmissions. Fully operational offline or aboard aircraft."
                    else
                        "すべてのデータはお使いの端末内ローカルストレージにのみ保存され、外部サーバーへの通信は一切発生しません。飛行機内やオフライン環境でも100%確実に動作します。"
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
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stepNumber,
                    color = com.example.checklist.theme.SpaceXAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .border(1.dp, com.example.checklist.theme.SpaceXAccent, com.example.checklist.ui.components.SpaceXSmallShape)
                        .background(SurfaceElevated, shape = com.example.checklist.ui.components.SpaceXSmallShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.3.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
