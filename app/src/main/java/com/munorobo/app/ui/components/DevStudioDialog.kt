package com.munorobo.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.munorobo.app.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DevStudioDialog(
    shape: RobotHeadShape,
    state: RobotHeadState,
    colors: RobotColors,
    speed: Float,
    antenna: Boolean,
    onApplyConfig: (
        newShape: RobotHeadShape?,
        newState: RobotHeadState?,
        newColors: RobotColors?,
        newSpeed: Float?,
        newAntenna: Boolean?,
        poke: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabIcons = listOf(
        Icons.Filled.AltRoute,
        Icons.Filled.Widgets,
        Icons.Filled.DataObject,
        Icons.Filled.Folder
    )
    val tabTitles = listOf("Flow", "Blocks", "JSON", "Projects")

    val initialBlocks = remember {
        listOf(
            SketchwareBlock("b1", "Happy Bounce", "Action", Color(0xFF4CAF50), state = RobotHeadState.HAPPY, speed = 1.5f, poke = true),
            SketchwareBlock("b2", "Matrix Hacker", "Setter", Color(0xFF00E676), shape = RobotHeadShape.HEXAGON, state = RobotHeadState.SEARCHING, shellColor = Color(0xFF3A3D44), screenColor = Color(0xFF7DFF9A)),
            SketchwareBlock("b3", "Deep Thinking", "Action", Color(0xFF2196F3), state = RobotHeadState.THINKING, speed = 1.0f),
            SketchwareBlock("b4", "Neon Pulse", "Setter", Color(0xFFE91E63), shape = RobotHeadShape.CIRCLE, state = RobotHeadState.SPEAKING, shellColor = Color(0xFF6A45B8), screenColor = Color(0xFFFF8FD0)),
            SketchwareBlock("b5", "System Alert", "Action", Color(0xFFF44336), state = RobotHeadState.ERROR, speed = 2.0f, poke = true)
        )
    }

    var blocks by remember { mutableStateOf(initialBlocks) }
    var flowSequence by remember { mutableStateOf(listOf(initialBlocks[0], initialBlocks[1])) }
    var isRunningFlow by remember { mutableStateOf(false) }

    var jsonText by remember {
        mutableStateOf(generatePhaseJson(shape, state, colors, speed, antenna))
    }

    val initialProjects = remember {
        listOf(
            SavedProject("p1", "Cobalt Classic", "Today", RobotHeadShape.RECTANGLE, RobotHeadState.IDLE, RobotColors(), 1.0f, true),
            SavedProject("p2", "Cyber Matrix", "Today", RobotHeadShape.HEXAGON, RobotHeadState.SEARCHING, RobotColors(shell = Color(0xFF3A3D44), screen = Color(0xFF7DFF9A)), 1.5f, true)
        )
    }
    var projects by remember { mutableStateOf(initialProjects) }
    var projectNameInput by remember { mutableStateOf("") }

    LaunchedEffect(shape, state, colors, speed, antenna) {
        jsonText = generatePhaseJson(shape, state, colors, speed, antenna)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dialog Header
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
                            imageVector = Icons.Filled.Code,
                            contentDescription = "Muno Studio",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Muno Studio",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Material 3 Tabs with Icons (NO EMOJIS)
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = tabIcons[index],
                                    contentDescription = title,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                // Tab Content
                when (selectedTab) {
                    0 -> {
                        // TAB 0: Flow
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sequence Execution",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = {
                                        if (!isRunningFlow && flowSequence.isNotEmpty()) {
                                            coroutineScope.launch {
                                                isRunningFlow = true
                                                for (b in flowSequence) {
                                                    onApplyConfig(
                                                        b.shape,
                                                        b.state,
                                                        if (b.shellColor != null || b.screenColor != null) {
                                                            colors.copy(
                                                                shell = b.shellColor ?: colors.shell,
                                                                screen = b.screenColor ?: colors.screen
                                                            )
                                                        } else null,
                                                        b.speed,
                                                        null,
                                                        b.poke
                                                    )
                                                    delay(900)
                                                }
                                                isRunningFlow = false
                                            }
                                        }
                                    },
                                    enabled = !isRunningFlow && flowSequence.isNotEmpty(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isRunningFlow) "Running..." else "Run Flow", fontSize = 11.sp)
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                item {
                                    Surface(
                                        color = Color(0xFFF59E0B),
                                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Flag,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text("On Trigger", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }

                                itemsIndexed(flowSequence) { index, block ->
                                    Surface(
                                        color = block.color.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, block.color),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(block.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = block.color)
                                                Text(
                                                    listOfNotNull(
                                                        block.shape?.let { "shape: ${it.label}" },
                                                        block.state?.let { "state: ${it.label}" },
                                                        if (block.poke) "hop & spin" else null
                                                    ).joinToString(" • "),
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    flowSequence = flowSequence.filterIndexed { i, _ -> i != index }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Remove",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: Blocks Library
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(blocks) { b ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    tonalElevation = 2.dp,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, b.color.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(b.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(b.tag, fontSize = 10.sp, color = b.color)
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            FilledTonalIconButton(
                                                onClick = {
                                                    onApplyConfig(b.shape, b.state, null, b.speed, null, b.poke)
                                                },
                                                modifier = Modifier.size(32.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Icon(Icons.Filled.PlayArrow, contentDescription = "Run", modifier = Modifier.size(16.dp))
                                            }

                                            FilledTonalIconButton(
                                                onClick = { flowSequence = flowSequence + b },
                                                modifier = Modifier.size(32.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Icon(Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: Phase JSON
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        applyPhaseJson(jsonText, onApplyConfig)
                                        Toast.makeText(context, "Applied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                FilledTonalButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(jsonText))
                                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedTextField(
                                value = jsonText,
                                onValueChange = { jsonText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    3 -> {
                        // TAB 3: Projects
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Current Session Card with fixed inline Save Project button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Current Setup", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = projectNameInput,
                                            onValueChange = { projectNameInput = it },
                                            placeholder = { Text("Project Name", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                            shape = RoundedCornerShape(8.dp)
                                        )

                                        Button(
                                            onClick = {
                                                val name = projectNameInput.trim().ifEmpty { "Project #${projects.size + 1}" }
                                                val newProj = SavedProject(
                                                    id = "p_${System.currentTimeMillis()}",
                                                    name = name,
                                                    date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date()),
                                                    shape = shape,
                                                    state = state,
                                                    colors = colors,
                                                    speed = speed,
                                                    antenna = antenna
                                                )
                                                projects = listOf(newProj) + projects
                                                projectNameInput = ""
                                                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            // Saved Projects List
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(projects) { p ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        tonalElevation = 1.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(p.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("${p.shape.label} • ${p.state.label}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                FilledTonalButton(
                                                    onClick = {
                                                        onApplyConfig(p.shape, p.state, p.colors, p.speed, p.antenna, false)
                                                        Toast.makeText(context, "Loaded", Toast.LENGTH_SHORT).show()
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("Load", fontSize = 10.sp)
                                                }

                                                IconButton(
                                                    onClick = { projects = projects.filter { it.id != p.id } },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generatePhaseJson(
    shape: RobotHeadShape,
    state: RobotHeadState,
    colors: RobotColors,
    speed: Float,
    antenna: Boolean
): String {
    return """
{
  "component": "Muno",
  "version": "1.0",
  "activeConfig": {
    "shape": "${shape.label.lowercase()}",
    "state": "${state.label.lowercase()}",
    "speed": $speed,
    "antenna": $antenna
  },
  "assets": {
    "shapes": ["rectangle", "square", "circle", "hexagon"],
    "emotions": ["idle", "thinking", "searching", "listening", "speaking", "working", "happy", "error", "sleeping"],
    "ledGrid": "16x8"
  }
}
    """.trimIndent()
}

private fun applyPhaseJson(
    json: String,
    onApply: (RobotHeadShape?, RobotHeadState?, RobotColors?, Float?, Boolean?, Boolean) -> Unit
) {
    try {
        var shape: RobotHeadShape? = null
        var state: RobotHeadState? = null
        var speed: Float? = null
        var antenna: Boolean? = null

        if (json.contains("\"shape\": \"square\"", ignoreCase = true)) shape = RobotHeadShape.SQUARE
        if (json.contains("\"shape\": \"circle\"", ignoreCase = true)) shape = RobotHeadShape.CIRCLE
        if (json.contains("\"shape\": \"hexagon\"", ignoreCase = true)) shape = RobotHeadShape.HEXAGON
        if (json.contains("\"shape\": \"rectangle\"", ignoreCase = true)) shape = RobotHeadShape.RECTANGLE

        RobotHeadState.entries.forEach { st ->
            if (json.contains("\"state\": \"${st.label.lowercase()}\"", ignoreCase = true)) {
                state = st
            }
        }

        if (json.contains("\"antenna\": false", ignoreCase = true)) antenna = false
        if (json.contains("\"antenna\": true", ignoreCase = true)) antenna = true

        onApply(shape, state, null, speed, antenna, true)
    } catch (_: Exception) {}
}
