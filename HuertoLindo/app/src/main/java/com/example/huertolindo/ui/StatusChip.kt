package com.example.huertolindo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun HuertoStatusChip(status: String, modifier: Modifier = Modifier) {
    val colors = when (status.lowercase()) {
        "completada", "resuelto", "listo para cosechar", "disponible" ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "rechazada", "alta", "atrasada", "inactivo" ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        "pendiente", "media", "en uso" ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer to
            MaterialTheme.colorScheme.onSecondaryContainer
    }
    val containerColor by animateColorAsState(
        colors.first,
        animationSpec = tween(220),
        label = "status-container"
    )
    val contentColor by animateColorAsState(
        colors.second,
        animationSpec = tween(220),
        label = "status-content"
    )

    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50)
    ) {
        AnimatedContent(targetState = status, label = "status-label") { animatedStatus ->
            Text(
                text = animatedStatus,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
