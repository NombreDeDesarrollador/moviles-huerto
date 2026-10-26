package com.example.huertolindo.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun DashboardActionGrid(actions: List<DashboardActionItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        actions.chunked(2).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowActions.forEach { action ->
                    DashboardAction(action, Modifier.weight(1f))
                }
                if (rowActions.size == 1) {
                    Box(Modifier.weight(1f))
                }
            }
        }
    }
}

internal data class DashboardActionItem(
    val title: String,
    val illustration: String,
    val onClick: () -> Unit
)

@Composable
private fun DashboardAction(action: DashboardActionItem, modifier: Modifier = Modifier) {
    val cardShape = MaterialTheme.shapes.medium
    ElevatedCard(
        onClick = action.onClick,
        modifier = modifier
            .aspectRatio(1f)
            .animateContentSize()
            .border(BorderStroke(0.75.dp, Color.Black), cardShape)
            .semantics { contentDescription = action.title },
        shape = cardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(action.illustration, fontSize = 40.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                action.title,
                style = MaterialTheme.typography.titleSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
