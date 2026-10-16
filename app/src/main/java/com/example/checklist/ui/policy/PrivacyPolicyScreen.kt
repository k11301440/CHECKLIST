package com.example.checklist.ui.policy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.checklist.theme.BorderSubtle
import com.example.checklist.theme.SpaceBlack
import com.example.checklist.theme.SurfaceDark
import com.example.checklist.theme.TextPrimary
import com.example.checklist.theme.TextSecondary
import com.example.checklist.theme.TextTertiary
import com.example.checklist.ui.components.SpaceXTopBar

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        SpaceXTopBar(
            title = "PRIVACY POLICY",
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
                    text = "DATA TELEMETRY & PRIVACY PRINCIPLES",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "ZERO EXTERNAL TELEMETRY",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                PolicySection(
                    title = "1. 基本方針 (Philosophy)",
                    body = "本アプリ「Flight Checklist」は、航空のチェックリスト思想およびSpaceXのデザイン哲学に基づき設計された個人用デイリーチェックリストアプリです。プライバシーの保護とオフラインの確実性を最優先とし、外部サーバーとの通信は一切行いません。"
                )

                PolicySection(
                    title = "2. 収集される情報 (Data Collection)",
                    body = "本アプリは、氏名、メールアドレス、位置情報、端末識別子、行動ログなどの個人情報を一切収集しません。また、インターネットアクセス権限（android.permission.INTERNET）を要求しておらず、物理的に外部送信が不可能な構造になっています。"
                )

                PolicySection(
                    title = "3. データの保存場所 (Data Storage)",
                    body = "ユーザーが作成したタスク、フェーズ、テンプレート、振り返りメモ、およびカレンダー履歴データは、すべてご利用の端末内の保護されたローカルストレージ（内部ストレージ）にのみ保存されます。"
                )

                PolicySection(
                    title = "4. 第三者提供および広告 (Third-Party & Ads)",
                    body = "本アプリには広告配信SDK、アナリティクスSDK、トラッカー等は一切含まれていません。したがって、第三者へのデータ提供やトラッキングが行われることはありません。"
                )

                PolicySection(
                    title = "5. データの管理と削除 (Data Deletion)",
                    body = "保存されたデータは、設定画面内の「初期化」操作、または端末の設定から「アプリのデータを消去」もしくはアプリのアンインストールを行うことで、完全に削除されます。"
                )

                PolicySection(
                    title = "6. お問い合わせ (Contact)",
                    body = "プライバシーポリシーや本アプリに関するお問い合わせは、GitHubリポジトリ（https://github.com/k11301440/flight-checklist）のIssueよりご連絡ください。"
                )

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    body: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
            .background(SurfaceDark)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.3.sp
            )
        }
    }
}
