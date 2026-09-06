package com.globalpulse.news.presentation.news.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.globalpulse.news.domain.model.Market
import com.globalpulse.news.presentation.theme.Gold

private data class MarketTab(val label: String, val market: Market?)

private val tabs = listOf(
    MarketTab("الكل", null),
    MarketTab("🇪🇬 EGX", Market.EGX),
    MarketTab("🇺🇸 US", Market.US)
)

@Composable
fun MarketTabs(
    selected: Market?,
    onSelect: (Market?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { tab ->
            FilterChip(
                selected = selected == tab.market,
                onClick = { onSelect(tab.market) },
                label = { Text(tab.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Gold.copy(alpha = 0.15f),
                    selectedLabelColor = Gold
                )
            )
        }
    }
}
