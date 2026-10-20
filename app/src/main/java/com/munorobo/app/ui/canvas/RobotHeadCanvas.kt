package com.munorobo.app.ui.canvas

import android.graphics.Paint
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.munorobo.app.model.*
import kotlinx.coroutines.launch
import kotlin.math.*

@Composable
fun RobotHeadCanvas(
    shape: RobotHeadShape,
    state: RobotHeadState,
    colors: RobotColors,
    modifier: Modifier = Modifier,
    antenna: Boolean = true,
    speed: Float = 1.0f,
    isPaused: Boolean = false,
    floorShadow: Boolean = true,
    interactive: Boolean = true,
    zoomScaleState: MutableState<Float>? = null,
    onPoke: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val hopOffsetY = remember { Animatable(0f) }
    val spinYaw = remember { Animatable(0f) }

    // 3D Orientation (Yaw & Pitch) controlled directly by finger drag
    var userYaw by remember { mutableFloatStateOf(0f) }
    var userPitch by remember { mutableFloatStateOf(0f) }
    var zoomScale by (zoomScaleState ?: remember { mutableFloatStateOf(1.0f) })
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val infiniteTransition = rememberInfiniteTransition(label = "robot_clock")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "clock"
    )

    val time = if (isPaused) 0f else animProgress * speed

    val triggerPoke = {
        if (!hopOffsetY.isRunning && !spinYaw.isRunning) {
            coroutineScope.launch {
                launch {
                    hopOffsetY.animateTo(-0.25f, tween(180, easing = FastOutSlowInEasing))
                    hopOffsetY.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 380f))
                }
                launch {
                    spinYaw.snapTo(0f)
                    spinYaw.animateTo(Robot3DGeometry.TAU, tween(550, easing = FastOutSlowInEasing))
                    spinYaw.snapTo(0f)
                }
            }
            onPoke()
        }
    }

    Box(
        modifier = modifier.then(
            if (interactive) {
                Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { triggerPoke() },
                            onDoubleTap = {
                                userYaw = 0f
                                userPitch = 0f
                                zoomScale = 1.0f
                                panOffset = Offset.Zero
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            // Pinch zoom
                            zoomScale = (zoomScale * zoom).coerceIn(0.5f, 3.2f)
                            // 3D rotation via finger drag: horizontal rotates yaw, vertical rotates pitch
                            userYaw = (userYaw + pan.x * 0.007f).coerceIn(-1.2f, 1.2f)
                            userPitch = (userPitch - pan.y * 0.007f).coerceIn(-0.85f, 0.85f)
                            panOffset += pan * 0.15f
                        }
                    }
            } else Modifier
        )
    ) {
        val shape3D = remember(shape) { Robot3DGeometry.getShape(shape) }
        val meshes = remember(shape) { Robot3DGeometry.headMeshes(shape3D) }

        val shellMat = remember(colors.shell) {
            Material3D(
                albedo = Robot3DLight.colorToLinear(colors.shell),
                f0 = 0.05f,
                metal = 0.15f,
                spec = 0.55f,
                shininess = 70f,
                rim = 0.9f
            )
        }
        val trimMat = remember(colors.trim) {
            Material3D(
                albedo = Robot3DLight.colorToLinear(colors.trim),
                f0 = 0.2f,
                metal = 0.65f,
                spec = 0.8f,
                shininess = 45f,
                rim = 0.7f
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Calculate unit scale in screen pixels
            val maxHeadSpan = max(2f * shape3D.hx + 0.35f, 2f * shape3D.hy + 0.45f)
            val unit = (min(width, height) / maxHeadSpan) * 0.9f * zoomScale

            val cx = width / 2f + panOffset.x
            val cy = height / 2f + panOffset.y

            // State specific natural motion
            val idleYaw = if (!isPaused) sin(time * 0.9f) * 0.12f else 0f
            val idlePitch = if (!isPaused) cos(time * 0.6f) * 0.06f else 0f
            val idleBob = if (!isPaused) sin(time * 2.2f) * 0.02f else 0f

            val stateTilt = when (state) {
                RobotHeadState.THINKING -> -0.15f
                RobotHeadState.LISTENING -> 0.14f
                RobotHeadState.ERROR -> sin(time * 24f) * 0.1f
                RobotHeadState.HAPPY -> sin(time * 7f) * 0.06f
                else -> 0f
            }

            val stateHop = when (state) {
                RobotHeadState.HAPPY -> abs(sin(time * 6f)) * -0.09f
                RobotHeadState.IDLE -> if ((time % 6f) < 0.35f) sin((time % 6f) / 0.35f * Math.PI.toFloat()) * -0.06f else idleBob
                else -> idleBob
            }

            val totalYaw = userYaw + idleYaw + spinYaw.value
            val totalPitch = userPitch + idlePitch
            val totalRoll = stateTilt
            val totalLift = hopOffsetY.value + stateHop

            val rMatrix = Robot3DGeometry.rotation(totalYaw, totalPitch, totalRoll)

            // Floor Shadow
            if (floorShadow) {
                val shadowR = shape3D.hx * unit * 1.25f
                val shadowH = shadowR * 0.25f
                val shadowY = cy + (shape3D.hy + 0.16f) * unit + totalLift * unit * 0.2f
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x55000000), Color(0x18000000), Color.Transparent),
                        center = Offset(cx, shadowY),
                        radius = shadowR
                    ),
                    topLeft = Offset(cx - shadowR, shadowY - shadowH / 2f),
                    size = Size(shadowR * 2f, shadowH)
                )
            }

            // 3D Point projection function
            val project: (Float, Float, Float) -> FloatArray = { x, y, z ->
                val rot = Robot3DGeometry.rotate(rMatrix, x, y, z)
                floatArrayOf(
                    cx + rot[0] * unit,
                    cy + (rot[1] + totalLift) * unit,
                    rot[2]
                )
            }

            // Ear depth values to determine draw order
            val earLz = Robot3DGeometry.rotate(rMatrix, -shape3D.earX, Robot3DGeometry.HeadConstants.earY, 0f)[2]
            val earRz = Robot3DGeometry.rotate(rMatrix, shape3D.earX, Robot3DGeometry.HeadConstants.earY, 0f)[2]
            val topZ = Robot3DGeometry.rotate(rMatrix, 0f, -shape3D.hy, 0f)[2]
            val antennaBehind = topZ < -0.015f

            // 1. Draw Ear behind if facing away
            if (earLz < 0f) drawMesh(meshes.earL, trimMat, project, rMatrix, unit)
            if (earRz < 0f) drawMesh(meshes.earR, trimMat, project, rMatrix, unit)

            // 2. Draw Antenna behind if head tilted back
            if (antennaBehind && antenna) {
                drawAntenna3D(shape3D, project, colors.trim, colors.screen, unit, time, state)
            }

            // 3. Draw Main Head Shell 3D Quad Mesh
            drawMesh(meshes.body, shellMat, project, rMatrix, unit)

            // 4. Draw Front Plate, Recessed Bezel & LED Matrix Face
            val frontZ = rMatrix[8]
            if (frontZ > 0.02f) {
                drawFrontFace(shape3D, project, colors, state, time, unit, frontZ)
            }

            // 5. Draw Ear in front
            if (earLz >= 0f) drawMesh(meshes.earL, trimMat, project, rMatrix, unit)
            if (earRz >= 0f) drawMesh(meshes.earR, trimMat, project, rMatrix, unit)

            // 6. Draw Antenna in front
            if (!antennaBehind && antenna) {
                drawAntenna3D(shape3D, project, colors.trim, colors.screen, unit, time, state)
            }
        }
    }
}

private fun DrawScope.drawMesh(
    mesh: Mesh,
    material: Material3D,
    project: (Float, Float, Float) -> FloatArray,
    rMatrix: FloatArray,
    unit: Float
) {
    val numQuads = mesh.quads.size / 4
    val projected = FloatArray(mesh.pos.size)
    val numVertices = mesh.pos.size / 3

    for (i in 0 until numVertices) {
        val p = project(mesh.pos[i * 3], mesh.pos[i * 3 + 1], mesh.pos[i * 3 + 2])
        projected[i * 3] = p[0]
        projected[i * 3 + 1] = p[1]
        projected[i * 3 + 2] = p[2]
    }

    val visibleQuads = ArrayList<Pair<Float, Int>>()
    for (k in 0 until numQuads) {
        val nx = mesh.normals[k * 3]
        val ny = mesh.normals[k * 3 + 1]
        val nz = mesh.normals[k * 3 + 2]
        // Back-face culling using rotated Z normal
        val rotatedNz = rMatrix[6] * nx + rMatrix[7] * ny + rMatrix[8] * nz
        if (rotatedNz < -0.04f) continue

        val zSum = projected[mesh.quads[k * 4] * 3 + 2] +
                projected[mesh.quads[k * 4 + 1] * 3 + 2] +
                projected[mesh.quads[k * 4 + 2] * 3 + 2] +
                projected[mesh.quads[k * 4 + 3] * 3 + 2]
        visibleQuads.add(Pair(zSum, k))
    }

    // Depth sort back to front
    visibleQuads.sortBy { it.first }

    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = max(0.5f, unit / 280f)
            strokeJoin = Paint.Join.ROUND
        }

        val path = android.graphics.Path()

        for ((_, k) in visibleQuads) {
            val nx = mesh.normals[k * 3]
            val ny = mesh.normals[k * 3 + 1]
            val nz = mesh.normals[k * 3 + 2]
            val worldNorm = Robot3DGeometry.rotate(rMatrix, nx, ny, nz)

            val color = Robot3DLight.shade(material, worldNorm[0], worldNorm[1], worldNorm[2])
            paint.color = color.toArgb()

            path.rewind()
            for (j in 0 until 4) {
                val vi = mesh.quads[k * 4 + j] * 3
                val px = projected[vi]
                val py = projected[vi + 1]
                if (j == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()

            nativeCanvas.drawPath(path, paint)
        }
    }
}

private fun DrawScope.drawFrontFace(
    shape: Shape3D,
    project: (Float, Float, Float) -> FloatArray,
    colors: RobotColors,
    state: RobotHeadState,
    time: Float,
    unit: Float,
    frontZ: Float
) {
    val zf = shape.depth / 2f
    val centerP = project(0f, 0f, zf)

    // Screen bezel dimensions
    val gw = 2f * (shape.hx - Robot3DGeometry.InsetConstants.glass)
    val gh = 2f * (shape.hy - Robot3DGeometry.InsetConstants.glass)
    val screenW = gw * unit
    val screenH = gh * unit

    val sx = centerP[0] - screenW / 2f
    val sy = centerP[1] - screenH / 2f
    val cornerR = shape.r * unit * 0.6f

    // Recessed dark bezel
    drawRoundRect(
        color = Color(0xFF0F1420),
        topLeft = Offset(sx, sy),
        size = Size(screenW, screenH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR, cornerR)
    )

    // Deep LCD glass area
    val pad = max(3f, unit * 0.02f)
    val lcdW = screenW - pad * 2f
    val lcdH = screenH - pad * 2f
    val lcdX = sx + pad
    val lcdY = sy + pad

    drawRoundRect(
        color = Color(0xFF070B14),
        topLeft = Offset(lcdX, lcdY),
        size = Size(lcdW, lcdH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR * 0.8f, cornerR * 0.8f)
    )

    // 16 cols x 8 rows LED Matrix Grid
    val cols = 16
    val rows = 8
    val cellW = lcdW / cols
    val cellH = lcdH / rows
    val pixelRadius = min(cellW, cellH) * 0.33f

    val matrix = Array(rows) { BooleanArray(cols) { false } }
    populateFaceMatrix(matrix, state, time)

    val activeColor = if (state == RobotHeadState.ERROR) Color(0xFFFF3333) else colors.screen
    val offColor = Color(0x15FFFFFF)

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val px = lcdX + c * cellW + cellW / 2f
            val py = lcdY + r * cellH + cellH / 2f
            val isOn = matrix[r][c]

            if (isOn) {
                // Pixel Glow Bloom
                drawCircle(
                    color = activeColor.copy(alpha = 0.32f),
                    radius = pixelRadius * 1.9f,
                    center = Offset(px, py)
                )
                // Solid Core LED
                drawCircle(
                    color = activeColor,
                    radius = pixelRadius,
                    center = Offset(px, py)
                )
            } else {
                // Dim unlit LED dot
                drawCircle(
                    color = offColor,
                    radius = pixelRadius * 0.7f,
                    center = Offset(px, py)
                )
            }
        }
    }
}

private fun DrawScope.drawAntenna3D(
    shape: Shape3D,
    project: (Float, Float, Float) -> FloatArray,
    trimColor: Color,
    screenColor: Color,
    unit: Float,
    time: Float,
    state: RobotHeadState
) {
    val baseP = project(0f, -shape.hy, 0f)
    val tipP = project(0f, -shape.hy - Robot3DGeometry.HeadConstants.stem, 0f)

    // Stem line in 3D
    drawLine(
        color = trimColor,
        start = Offset(baseP[0], baseP[1]),
        end = Offset(tipP[0], tipP[1]),
        strokeWidth = max(3f, unit * 0.022f),
        cap = StrokeCap.Round
    )

    // Glowing Bulb in 3D
    val bulbRadius = Robot3DGeometry.HeadConstants.ball * unit * 1.35f
    val pulse = when (state) {
        RobotHeadState.ERROR -> abs(sin(time * 12f))
        RobotHeadState.THINKING -> abs(sin(time * 4f))
        RobotHeadState.HAPPY -> 0.85f + abs(sin(time * 6f)) * 0.25f
        else -> 0.65f + abs(sin(time * 2f)) * 0.35f
    }

    val bulbColor = if (state == RobotHeadState.ERROR) Color(0xFFFF3333) else screenColor

    drawCircle(
        color = bulbColor.copy(alpha = 0.35f * pulse),
        radius = bulbRadius * (1.6f + pulse * 0.5f),
        center = Offset(tipP[0], tipP[1])
    )
    drawCircle(
        color = bulbColor,
        radius = bulbRadius,
        center = Offset(tipP[0], tipP[1])
    )
}

private fun populateFaceMatrix(
    matrix: Array<BooleanArray>,
    state: RobotHeadState,
    time: Float
) {
    val blinkCycle = time % 3.5f
    val isBlinking = blinkCycle < 0.16f && state != RobotHeadState.SLEEPING && state != RobotHeadState.ERROR

    when (state) {
        RobotHeadState.IDLE -> {
            if (isBlinking) {
                for (c in 3..5) matrix[3][c] = true
                for (c in 10..12) matrix[3][c] = true
            } else {
                drawEye(matrix, 4, 3)
                drawEye(matrix, 11, 3)
            }
            matrix[6][6] = true; matrix[6][7] = true; matrix[6][8] = true; matrix[6][9] = true
            matrix[5][5] = true; matrix[5][10] = true
        }

        RobotHeadState.THINKING -> {
            drawEye(matrix, 4, 2)
            drawEye(matrix, 11, 2)

            val dot = (time * 4f).toInt() % 3
            if (dot == 0) matrix[1][7] = true
            if (dot == 1) matrix[1][8] = true
            if (dot == 2) matrix[1][9] = true

            matrix[6][7] = true; matrix[6][8] = true
        }

        RobotHeadState.SEARCHING -> {
            val beamCol = ((sin(time * 3f) + 1f) / 2f * 15f).roundToInt().coerceIn(0, 15)
            for (r in 0..7) matrix[r][beamCol] = true
            val pupilCol = beamCol.coerceIn(2, 13)
            matrix[3][pupilCol] = true
            matrix[4][pupilCol] = true
        }

        RobotHeadState.LISTENING -> {
            val h1 = (abs(sin(time * 6f)) * 5f).toInt()
            val h2 = (abs(sin(time * 8f + 1f)) * 6f).toInt()
            val h3 = (abs(sin(time * 5f + 2f)) * 7f).toInt()
            val h4 = (abs(sin(time * 7f + 3f)) * 6f).toInt()
            val h5 = (abs(sin(time * 9f + 4f)) * 5f).toInt()

            drawEqCol(matrix, 3, h1)
            drawEqCol(matrix, 6, h2)
            drawEqCol(matrix, 8, h3)
            drawEqCol(matrix, 10, h4)
            drawEqCol(matrix, 12, h5)
        }

        RobotHeadState.SPEAKING -> {
            drawEye(matrix, 4, 2)
            drawEye(matrix, 11, 2)

            val mouthOpen = (abs(sin(time * 10f)) * 3f).toInt()
            for (c in 5..10) {
                matrix[5][c] = true
                if (mouthOpen >= 1) matrix[6][c] = true
                if (mouthOpen >= 2) matrix[7][c] = true
            }
        }

        RobotHeadState.WORKING -> {
            val spinAngle = ((time * 8f) % 8f).toInt()
            val coords = listOf(
                Pair(1, 7), Pair(2, 9), Pair(3, 10), Pair(4, 9),
                Pair(5, 7), Pair(4, 5), Pair(3, 4), Pair(2, 5)
            )
            coords.forEachIndexed { i, pt ->
                if ((i + spinAngle) % 8 < 5) matrix[pt.first][pt.second] = true
            }
            val prog = ((time * 4f) % 16f).toInt()
            for (c in 0..prog) matrix[7][c] = true
        }

        RobotHeadState.HAPPY -> {
            matrix[3][3] = true; matrix[2][4] = true; matrix[3][5] = true
            matrix[3][10] = true; matrix[2][11] = true; matrix[3][12] = true

            for (c in 5..10) matrix[6][c] = true
            matrix[5][4] = true; matrix[5][11] = true
            matrix[4][3] = true; matrix[4][12] = true
        }

        RobotHeadState.ERROR -> {
            matrix[2][3] = true; matrix[2][5] = true
            matrix[3][4] = true
            matrix[4][3] = true; matrix[4][5] = true

            matrix[2][10] = true; matrix[2][12] = true
            matrix[3][11] = true
            matrix[4][10] = true; matrix[4][12] = true

            matrix[6][5] = true; matrix[7][6] = true; matrix[6][7] = true
            matrix[7][8] = true; matrix[6][9] = true; matrix[7][10] = true
        }

        RobotHeadState.SLEEPING -> {
            for (c in 3..5) matrix[4][c] = true
            for (c in 10..12) matrix[4][c] = true

            val zStep = (time * 2.5f) % 3f
            if (zStep > 0f) matrix[6][8] = true
            if (zStep > 1f) matrix[4][14] = true
            if (zStep > 2f) matrix[1][15] = true
        }
    }
}

private fun drawEye(matrix: Array<BooleanArray>, cx: Int, cy: Int) {
    for (r in cy - 1..cy + 1) {
        for (c in cx - 1..cx + 1) {
            if (r in 0..7 && c in 0..15) {
                matrix[r][c] = true
            }
        }
    }
}

private fun drawEqCol(matrix: Array<BooleanArray>, col: Int, height: Int) {
    val h = height.coerceIn(0, 7)
    for (r in (7 - h)..7) {
        if (col in 0..15 && r in 0..7) {
            matrix[r][col] = true
        }
    }
}
