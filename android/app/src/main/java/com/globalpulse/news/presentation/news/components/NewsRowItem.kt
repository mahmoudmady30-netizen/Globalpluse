package com.globalpulse.news.presentation.news.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.globalpulse.news.domain.model.NewsItem
import com.globalpulse.news.domain.model.VerificationStatus
import com.globalpulse.news.presentation.theme.Border
import com.globalpulse.news.presentation.theme.Bullish
import com.globalpulse.news.presentation.theme.Gold
import com.globalpulse.news.presentation.theme.TextPrimary
import com.globalpulse.news.presentation.theme.TextSecondary
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")

@Composable
fun NewsRowItem(item: NewsItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SentimentDot(item.primaryTicker?.sentimentLabel)
            Spacer(Modifier.width(10.dp))

            item.tickers.forEach { ticker ->
                Text(
                    text = ticker.symbol,
                    color = Gold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            VerificationBadge(item.verificationStatus, item.corroboratingSources.size)
        }

        Text(
            text = item.headline,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.sourceUrl)))
                }
        )

        Text(
            text = "${item.source} · ${item.publishedAt.format(timeFormatter)} · ${item.dataStatus}",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (item.potentialImpact.isNotEmpty()) {
            Text(
                text = "تأثير محتمل: ${item.potentialImpact.joinToString(" · ")}",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }

    HorizontalDivider(color = Border, thickness = 1.dp)
}

@Composable
private fun VerificationBadge(status: VerificationStatus, sourceCount: Int) {
    val (label, color) = when (status) {
        VerificationStatus.MULTI_SOURCE_VERIFIED -> "متعدد المصادر ($sourceCount)" to Bullish
        VerificationStatus.CONFLICTING_REPORTS -> "تقارير متضاربة" to Color(0xFFC1554D)
        VerificationStatus.SINGLE_SOURCE -> "مصدر واحد" to TextSecondary
    }
    Text(
        text = label,
        color = color,
        fontSize = 11.sp,
        modifier = Modifier.wrapContentWidth()
    )
}
