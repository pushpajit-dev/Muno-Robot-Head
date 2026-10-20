package com.munorobo.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.munorobo.app.model.RobotColors
import com.munorobo.app.model.RobotHeadShape
import com.munorobo.app.model.RobotHeadState
import com.munorobo.app.ui.canvas.RobotHeadCanvas
import com.munorobo.app.ui.components.*

@Composable
fun MunoRoboApp() {
    val systemDark = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(true) }

    // Active robot parameters
    var shape by remember { mutableStateOf(RobotHeadShape.RECTANGLE) }
    var state by remember { mutableStateOf(RobotHeadState.IDLE) }
    var colors by remember { mutableStateOf(RobotColors()) }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var antenna by remember { mutableStateOf(true) }
    var isPaused by remember { mutableStateOf(false) }

    // Bottom Sheet visibility
    var showPropertiesSheet by remember { mutableStateOf(false) }

    // Dev Studio Dialog visibility (NOT shown on open, opens via button)
    var showDevDialog by remember { mutableStateOf(false) }

    // Zoom and 3D Reset trigger key
    var canvasKey by remember { mutableIntStateOf(0) }
    val zoomScaleState = remember(canvasKey) { mutableFloatStateOf(1.0f) }

    // High contrast Material 3 color schemes
    val darkColors = darkColorScheme(
        primary = Color(0xFF3B82F6),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF1E3A8A),
        onPrimaryContainer = Color(0xFFBFDBFE),
        surface = Color(0xFF12151C),
        onSurface = Color(0xFFF3F4F6),
        surfaceVariant = Color(0xFF1F2430),
        onSurfaceVariant = Color(0xFF9CA3AF),
        background = Color(0xFF0A0A0C),
        onBackground = Color(0xFFF3F4F6),
        outline = Color(0xFF374151)
    )

    val lightColors = lightColorScheme(
        primary = Color(0xFF2563EB),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDBEAFE),
        onPrimaryContainer = Color(0xFF1E40AF),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF111827),
        surfaceVariant = Color(0xFFF1F5F9),
        onSurfaceVariant = Color(0xFF4B5563),
        background = Color(0xFFF6F8FA),
        onBackground = Color(0xFF111827),
        outline = Color(0xFFCBD5E1)
    )

    MaterialTheme(colorScheme = if (isDarkTheme) darkColors else lightColors) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                // Top Bar with Material 3 icons only (NO text, NO emojis)
                MunoTopBar(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme },
                    onOpenProperties = { showPropertiesSheet = true },
                    onOpenDevDialog = { showDevDialog = true },
                    onResetZoomRotation = { canvasKey++ }
                )
            },
            bottomBar = {
                // Bottom animated emotions carousel
                EmotionsBar(
                    currentShape = shape,
                    currentState = state,
                    onSelectState = { state = it },
                    colors = colors,
                    antenna = antenna,
                    speed = speed,
                    isPaused = isPaused
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Center 3D Interactive Robot Head Stage (Supports 3D rotation, orientation, and pinch zoom)
                key(canvasKey) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RobotHeadCanvas(
                            shape = shape,
                            state = state,
                            colors = colors,
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .aspectRatio(1f),
                            antenna = antenna,
                            speed = speed,
                            isPaused = isPaused,
                            floorShadow = true,
                            interactive = true,
                            zoomScaleState = zoomScaleState
                        )
                    }
                }

                // Left Side: Floating Shape Dock (Clean shape buttons, NO text, NO emojis)
                ShapeDock(
                    currentShape = shape,
                    onSelectShape = { shape = it },
                    colors = colors,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                )
            }
        }

        // Material 3 Properties Bottom Sheet
        if (showPropertiesSheet) {
            PropertiesBottomSheet(
                colors = colors,
                onColorsChange = { colors = it },
                antenna = antenna,
                onAntennaChange = { antenna = it },
                speed = speed,
                onSpeedChange = { speed = it },
                isPaused = isPaused,
                onPausedChange = { isPaused = it },
                shape = shape,
                state = state,
                onReset = {
                    colors = RobotColors()
                    speed = 1.0f
                    antenna = true
                    isPaused = false
                },
                onDismiss = { showPropertiesSheet = false }
            )
        }

        // Material 3 Dev Studio Dialog (Opens via Code Icon Button)
        if (showDevDialog) {
            DevStudioDialog(
                shape = shape,
                state = state,
                colors = colors,
                speed = speed,
                antenna = antenna,
                onApplyConfig = { newShape, newState, newColors, newSpeed, newAntenna, _ ->
                    if (newShape != null) shape = newShape
                    if (newState != null) state = newState
                    if (newColors != null) colors = newColors
                    if (newSpeed != null) speed = newSpeed
                    if (newAntenna != null) antenna = newAntenna
                },
                onDismiss = { showDevDialog = false }
            )
        }
    }
}
