package org.n3gbx.whisper.ui.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.n3gbx.whisper.ui.utils.convertToTime

@Composable
fun TotalDuration(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    totalDuration: Long
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.height(14.dp),
            imageVector = Icons.Rounded.Headset,
            tint = color,
            contentDescription = "Headset"
        )
        Text(
            text = totalDuration.convertToTime(),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            color = color,
            style = MaterialTheme.typography.bodySmall
        )
    }
}