package com.hectordev.mvp.ui.core.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hectordev.mvp.R
import com.hectordev.mvp.domain.EventStatus
import com.hectordev.mvp.ui.core.theme.MVPTheme

@Composable
fun EventStatusChip(status: EventStatus, modifier: Modifier = Modifier) {
    val label = when (status) {
        EventStatus.PRE_TRIP -> stringResource(R.string.home_status_pre_trip)
        EventStatus.PREDICTION -> stringResource(R.string.home_status_prediction)
        EventStatus.ON_GOING -> stringResource(R.string.home_status_on_going)
        else -> return
    }
    val color = when (status) {
        EventStatus.ON_GOING -> MaterialTheme.colorScheme.primary
        EventStatus.PREDICTION -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
        contentColor = color
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// --- PREVIEWS ---

@Preview(showBackground = true, name = "Planning")
@Composable
private fun EventStatusChipPlanningPreview() {
    MVPTheme { EventStatusChip(status = EventStatus.PRE_TRIP) }
}

@Preview(showBackground = true, name = "Live")
@Composable
private fun EventStatusChipLivePreview() {
    MVPTheme { EventStatusChip(status = EventStatus.ON_GOING) }
}