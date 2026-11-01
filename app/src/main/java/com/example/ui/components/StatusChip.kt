package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CircuitStatus
import com.example.ui.theme.StatusEnergized
import com.example.ui.theme.StatusEnergizedBg
import com.example.ui.theme.StatusOutage
import com.example.ui.theme.StatusOutageBg
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusRotation
import com.example.ui.theme.StatusRotationBg

@Composable
fun StatusChip(
    status: CircuitStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, borderColor, textColor, label, dotColor) = when (status) {
        CircuitStatus.ENERGIZED -> StatusConfig(
            StatusEnergizedBg.copy(alpha = 0.7f),
            StatusEnergized,
            StatusEnergized,
            "Energizado",
            StatusEnergized
        )
        CircuitStatus.OUTAGE -> StatusConfig(
            StatusOutageBg.copy(alpha = 0.7f),
            StatusOutage,
            StatusOutage,
            "Sin Servicio",
            StatusOutage
        )
        CircuitStatus.ROTATION -> StatusConfig(
            StatusRotationBg.copy(alpha = 0.7f),
            StatusRotation,
            StatusRotation,
            "En Rotación",
            StatusRotation
        )
        CircuitStatus.UNKNOWN -> StatusConfig(
            StatusPendingBg.copy(alpha = 0.7f),
            StatusPending.copy(alpha = 0.6f),
            StatusPending,
            "En Espera",
            StatusPending
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .heightIn(min = 28.dp)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class StatusConfig(
    val bgColor: Color,
    val borderColor: Color,
    val textColor: Color,
    val label: String,
    val dotColor: Color
)
