package com.orbitx.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Small shared widgets used across the screens. */

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = OrbitText,
        )
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OrbitMuted)
        }
    }
}

@Composable
fun OrbitCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(OrbitSurface)
            .padding(contentPadding),
    ) { content() }
}

@Composable
fun Pill(text: String, color: Color = OrbitMint) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = OrbitMint,
            contentColor = Color(0xFF04150E),
            disabledContainerColor = OrbitSurfaceHi,
            disabledContentColor = OrbitMuted,
        ),
    ) { Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun GhostButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun ProgressRow(fraction: Float, label: String) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = OrbitMuted)
            Text("${(fraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = OrbitMint)
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 4.dp),
            color = OrbitMint,
            trackColor = OrbitSurfaceHi,
        )
    }
}

@Composable
fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(OrbitSurfaceHi))
}

@Composable
fun LabelValue(label: String, value: String, valueColor: Color = OrbitText) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = OrbitMuted)
        Text(value, style = MaterialTheme.typography.bodySmall, color = valueColor)
    }
}
