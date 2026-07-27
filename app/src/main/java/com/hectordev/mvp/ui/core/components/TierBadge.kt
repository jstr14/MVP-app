package com.hectordev.mvp.ui.core.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hectordev.mvp.R
import com.hectordev.mvp.domain.TimelineTier

@Composable
fun TimelineTier.displayName(): String = stringResource(
    when (this) {
        TimelineTier.FACT -> R.string.tier_fact
        TimelineTier.HOT_TAKE -> R.string.tier_hot_take
        TimelineTier.WITNESSED -> R.string.tier_witnessed
        TimelineTier.LORE -> R.string.tier_lore
    }
)

@Composable
fun TierBadge(tier: TimelineTier, modifier: Modifier = Modifier) {
    val (containerColor, contentColor) = when (tier) {
        TimelineTier.FACT -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        TimelineTier.HOT_TAKE -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        TimelineTier.WITNESSED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        TimelineTier.LORE -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Text(
            text = "${tier.displayName().uppercase()}  ${pluralStringResource(R.plurals.live_feed_tier_points, tier.points, tier.points)}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}