package com.globalpulse.news.presentation.news.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.globalpulse.news.domain.model.SentimentLabel
import com.globalpulse.news.presentation.theme.Bearish
import com.globalpulse.news.presentation.theme.Bullish
import com.globalpulse.news.presentation.theme.Neutral

@Composable
fun SentimentDot(label: SentimentLabel?, modifier: Modifier = Modifier) {
    val color = when (label) {
        SentimentLabel.BULLISH -> Bullish
        SentimentLabel.BEARISH -> Bearish
        SentimentLabel.NEUTRAL, null -> Neutral
    }
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}
