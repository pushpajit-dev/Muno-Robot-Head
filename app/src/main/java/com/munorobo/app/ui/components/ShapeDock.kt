package com.munorobo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.munorobo.app.model.RobotColors
import com.munorobo.app.model.RobotHeadShape
import com.munorobo.app.model.RobotHeadState
import com.munorobo.app.ui.canvas.RobotHeadCanvas

@Composable
fun ShapeDock(
    currentShape: RobotHeadShape,
    onSelectShape: (RobotHeadShape) -> Unit,
    colors: RobotColors,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RobotHeadShape.entries.forEach { shape ->
                val isSelected = shape == currentShape
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else Color.Transparent
                        )
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectShape(shape) },
                    contentAlignment = Alignment.Center
                ) {
                    RobotHeadCanvas(
                        shape = shape,
                        state = RobotHeadState.IDLE,
                        colors = colors,
                        modifier = Modifier.size(36.dp),
                        antenna = true,
                        speed = 0.8f,
                        isPaused = false,
                        floorShadow = false,
                        interactive = false,
                        isHighQuality = false
                    )
                }
            }
        }
    }
}
