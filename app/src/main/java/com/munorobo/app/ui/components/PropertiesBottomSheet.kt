package com.munorobo.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.munorobo.app.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesBottomSheet(
    colors: RobotColors,
    onColorsChange: (RobotColors) -> Unit,
    antenna: Boolean,
    onAntennaChange: (Boolean) -> Unit,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    isPaused: Boolean,
    onPausedChange: (Boolean) -> Unit,
    shape: RobotHeadShape,
    state: RobotHeadState,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Properties",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Properties",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                FilledTonalIconButton(
                    onClick = onReset,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Reset",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 1. Surface Texture & Finish Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Surface Texture & Finish",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(Swatches.textures) { tex ->
                        val isSelected = colors.texture == tex
                        TextureCard(
                            texture = tex,
                            isSelected = isSelected,
                            onClick = { onColorsChange(colors.copy(texture = tex)) }
                        )
                    }
                }
            }

            // 2. Shell Color Row (Horizontal Scroll)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Shell Color",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(Swatches.shellColors) { (name, color) ->
                        val isSelected = colors.shell == color
                        ColorSwatchCircle(
                            color = color,
                            label = name,
                            isSelected = isSelected,
                            onClick = { onColorsChange(colors.copy(shell = color)) }
                        )
                    }
                }
            }

            // 3. Trim Color Row (Horizontal Scroll)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Trim Color",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(Swatches.trimColors) { (name, color) ->
                        val isSelected = colors.trim == color
                        ColorSwatchCircle(
                            color = color,
                            label = name,
                            isSelected = isSelected,
                            onClick = { onColorsChange(colors.copy(trim = color)) }
                        )
                    }
                }
            }

            // 4. Screen Color Row (Horizontal Scroll)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Screen LED Color",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(Swatches.screenColors) { (name, color) ->
                        val isSelected = colors.screen == color
                        ColorSwatchCircle(
                            color = color,
                            label = name,
                            isSelected = isSelected,
                            onClick = { onColorsChange(colors.copy(screen = color)) }
                        )
                    }
                }
            }

            // 5. Controls Row (Play/Pause Icon, Antenna Icon, Speed Chips) in a Horizontal Scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Icon Button (Material 3 Icons - NO EMOJIS)
                FilledTonalIconButton(
                    onClick = { onPausedChange(!isPaused) },
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (isPaused) "Play" else "Pause",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Antenna Toggle Chip with Material 3 Icon
                FilterChip(
                    selected = antenna,
                    onClick = { onAntennaChange(!antenna) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Sensors,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(if (antenna) "Antenna ON" else "Antenna OFF", fontSize = 11.5.sp) },
                    shape = RoundedCornerShape(10.dp)
                )

                // Speed Chips (0.5x, 1x, 1.5x, 2x)
                Swatches.speeds.forEach { sp ->
                    FilterChip(
                        selected = speed == sp,
                        onClick = { onSpeedChange(sp) },
                        label = { Text("${sp}×", fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // 6. Code Snippet Card with Material 3 Copy Icon
            val codeSnippet = """
<RobotHead
  shape="${shape.label.lowercase()}"
  state="${state.label.lowercase()}"
  texture="${colors.texture.name.lowercase()}"
  antenna={$antenna}
  speed={$speed}
/>
            """.trimIndent()

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = codeSnippet,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FilledTonalIconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(codeSnippet))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(34.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy Code",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun TextureCard(
    texture: SurfaceTexture,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (texture) {
        SurfaceTexture.GLOSS -> Icons.Filled.AutoAwesome
        SurfaceTexture.METALLIC -> Icons.Filled.Shield
        SurfaceTexture.BRUSHED -> Icons.Filled.Tune
        SurfaceTexture.MATTE -> Icons.Filled.Palette
        SurfaceTexture.CARBON -> Icons.Filled.Grid4x4
        SurfaceTexture.HOLO -> Icons.Filled.BlurOn
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            width = if (isSelected) 1.8.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        modifier = Modifier.width(112.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = texture.label,
                    fontSize = 10.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                text = texture.description,
                fontSize = 8.5.sp,
                lineHeight = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun ColorSwatchCircle(
    color: Color,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    shape = CircleShape
                )
        )
        Text(
            text = label,
            fontSize = 9.5.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
