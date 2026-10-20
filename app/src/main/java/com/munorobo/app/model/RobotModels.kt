package com.munorobo.app.model

import androidx.compose.ui.graphics.Color

enum class RobotHeadShape(val label: String) {
    RECTANGLE("Rectangle"),
    SQUARE("Square"),
    CIRCLE("Circle"),
    HEXAGON("Hexagon")
}

enum class RobotHeadState(val label: String) {
    IDLE("Idle"),
    THINKING("Thinking"),
    SEARCHING("Searching"),
    LISTENING("Listening"),
    SPEAKING("Speaking"),
    WORKING("Working"),
    HAPPY("Happy"),
    ERROR("Error"),
    SLEEPING("Sleeping")
}

enum class SurfaceTexture(
    val label: String,
    val description: String,
    val metal: Float,
    val spec: Float,
    val shininess: Float,
    val f0: Float,
    val rim: Float,
    val isMetallic: Boolean = false,
    val isHolo: Boolean = false
) {
    GLOSS(
        label = "Smooth Gloss",
        description = "Moulded gloss with clear coat sheen",
        metal = 0.15f,
        spec = 0.65f,
        shininess = 70f,
        f0 = 0.05f,
        rim = 0.9f
    ),
    METALLIC(
        label = "Metallic Chrome",
        description = "Polished reflective metallic alloy",
        metal = 0.88f,
        spec = 1.45f,
        shininess = 115f,
        f0 = 0.45f,
        rim = 1.35f,
        isMetallic = true
    ),
    BRUSHED(
        label = "Brushed Metal",
        description = "Anodized satin metal with soft sheen",
        metal = 0.72f,
        spec = 0.95f,
        shininess = 40f,
        f0 = 0.28f,
        rim = 1.15f,
        isMetallic = true
    ),
    MATTE(
        label = "Matte Ceramic",
        description = "Velvety non-reflective ceramic",
        metal = 0.02f,
        spec = 0.22f,
        shininess = 18f,
        f0 = 0.02f,
        rim = 0.65f
    ),
    CARBON(
        label = "Cyber Carbon",
        description = "High-tech carbon with sharp highlights",
        metal = 0.50f,
        spec = 1.30f,
        shininess = 95f,
        f0 = 0.22f,
        rim = 1.45f
    ),
    HOLO(
        label = "Holo Prismatic",
        description = "Iridescent shifting rainbow reflections",
        metal = 0.38f,
        spec = 1.35f,
        shininess = 88f,
        f0 = 0.32f,
        rim = 1.95f,
        isHolo = true
    )
}

data class RobotColors(
    val shell: Color = Color(0xFF2B49A3),
    val trim: Color = Color(0xFF93A6C8),
    val screen: Color = Color(0xFFE8F2FF),
    val texture: SurfaceTexture = SurfaceTexture.GLOSS
)

object Swatches {
    val textures = SurfaceTexture.entries

    val shellColors = listOf(
        "Cobalt" to Color(0xFF2B49A3),
        "Tomato" to Color(0xFFC8372D),
        "Tangerine" to Color(0xFFE2762A),
        "Mustard" to Color(0xFFE0AD1F),
        "Mint" to Color(0xFF2F8F58),
        "Teal" to Color(0xFF1F8189),
        "Grape" to Color(0xFF6A45B8),
        "Bubblegum" to Color(0xFFD9558F),
        "Graphite" to Color(0xFF3A3D44),
        "Porcelain" to Color(0xFFE6E6EA)
    )

    val trimColors = listOf(
        "Steel" to Color(0xFF93A6C8),
        "Silver" to Color(0xFFC4C9D2),
        "Brass" to Color(0xFFC9A45C),
        "Copper" to Color(0xFFC27A52),
        "Black" to Color(0xFF2A2C31)
    )

    val screenColors = listOf(
        "White" to Color(0xFFE8F2FF),
        "Ice" to Color(0xFF7FE7FF),
        "Matrix" to Color(0xFF7DFF9A),
        "Amber" to Color(0xFFFFC46B),
        "Neon" to Color(0xFFFF8FD0)
    )

    val speeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)
}

data class SketchwareBlock(
    val id: String,
    val name: String,
    val tag: String,
    val color: Color,
    val shape: RobotHeadShape? = null,
    val state: RobotHeadState? = null,
    val shellColor: Color? = null,
    val screenColor: Color? = null,
    val speed: Float? = null,
    val poke: Boolean = false
)

data class SavedProject(
    val id: String,
    val name: String,
    val date: String,
    val shape: RobotHeadShape,
    val state: RobotHeadState,
    val colors: RobotColors,
    val speed: Float,
    val antenna: Boolean
)
