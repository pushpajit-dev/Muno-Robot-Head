package com.munorobo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.munorobo.app.model.RobotColors
import com.munorobo.app.model.RobotHeadShape
import com.munorobo.app.model.RobotHeadState
import com.munorobo.app.ui.canvas.RobotHeadCanvas

@Composable
fun EmotionsBar(
    currentShape: RobotHeadShape,
    currentState: RobotHeadState,
    onSelectState: (RobotHeadState) -> Unit,
    colors: RobotColors,
    antenna: Boolean,
    speed: Float,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 3.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(RobotHeadState.entries) { state ->
                val isSelected = state == currentState

                Column(
                    modifier = Modifier
                        .width(62.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            else Color.Transparent
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectState(state) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    RobotHeadCanvas(
                        shape = currentShape,
                        state = state,
                        colors = colors,
                        modifier = Modifier.size(46.dp),
                        antenna = antenna,
                        speed = speed,
                        isPaused = isPaused,
                        floorShadow = false,
                        interactive = false,
                        isHighQuality = false
                    )

                    Text(
                        text = state.label,
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
