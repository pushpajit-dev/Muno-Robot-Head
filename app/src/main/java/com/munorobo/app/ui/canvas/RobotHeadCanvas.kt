package com.munorobo.app.ui.canvas

import android.graphics.Paint
import android.graphics.RectF
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
                            userYaw = (userYaw + pan.x * 0.007f).coerceIn(-1.4f, 1.4f)
                            userPitch = (userPitch - pan.y * 0.007f).coerceIn(-0.95f, 0.95f)
                            panOffset += pan * 0.15f
                        }
                    }
            } else Modifier
        )
    ) {
        val shape3D = remember(shape) { Robot3DGeometry.getShape(shape) }
        val meshes = remember(shape) { Robot3DGeometry.headMeshes(shape3D) }

        val texture = colors.texture
        val shellMat = remember(colors.shell, texture) {
            Material3D(
                albedo = Robot3DLight.colorToLinear(colors.shell),
                f0 = texture.f0,
                metal = texture.metal,
                spec = texture.spec,
                shininess = texture.shininess,
                rim = texture.rim,
                isMetallic = texture.isMetallic,
                isHolo = texture.isHolo
            )
        }
        val trimMat = remember(colors.trim, texture) {
            Material3D(
                albedo = Robot3DLight.colorToLinear(colors.trim),
                f0 = if (texture.isMetallic) 0.35f else 0.2f,
                metal = if (texture.isMetallic) 0.85f else 0.65f,
                spec = if (texture.isMetallic) 1.25f else 0.8f,
                shininess = if (texture.isMetallic) 65f else 45f,
                rim = if (texture.isMetallic) 1.15f else 0.7f,
                isMetallic = texture.isMetallic
            )
        }

        val faceGrid = remember { Robot3DFaces.FaceGrid() }

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

            // Depths to determine draw order
            val earLz = Robot3DGeometry.rotate(rMatrix, -shape3D.earX, Robot3DGeometry.HeadConstants.earY, 0f)[2]
            val earRz = Robot3DGeometry.rotate(rMatrix, shape3D.earX, Robot3DGeometry.HeadConstants.earY, 0f)[2]
            val topZ = Robot3DGeometry.rotate(rMatrix, 0f, -shape3D.hy, 0f)[2]
            val antennaBehind = topZ < -0.015f

            val lightConfig = Robot3DFaces.getLightConfig(state)

            // Populate face grid for current state and time
            Robot3DFaces.populate(faceGrid, state, time)

            // 1. Draw Ear behind if facing away
            if (earLz < 0f) drawMesh(meshes.earL, trimMat, project, rMatrix, unit)
            if (earRz < 0f) drawMesh(meshes.earR, trimMat, project, rMatrix, unit)

            // 2. Draw Antenna & Collar behind if head tilted back
            if (antennaBehind) {
                drawMesh(meshes.collar, trimMat, project, rMatrix, unit)
                if (antenna) {
                    drawAntennaStemAndBulb(
                        shape3D = shape3D,
                        project = project,
                        trimColor = colors.trim,
                        lightConfig = lightConfig,
                        unit = unit,
                        time = time,
                        userYaw = userYaw,
                        userPitch = userPitch
                    )
                }
            }

            // 3. Draw Main Head Shell 3D Quad Mesh
            drawMesh(meshes.body, shellMat, project, rMatrix, unit)

            // 4. Draw 3D Front Plate, Recessed Bezel & LED Face (or Back Plate with Vents if rotated back)
            val frontZ = rMatrix[8]
            val zf = shape3D.depth / 2f

            if (frontZ > 0.005f) {
                draw3DFrontPlateAndFace(
                    shape3D = shape3D,
                    project = project,
                    shellMat = shellMat,
                    screenColor = lightConfig.screenColorOverride ?: colors.screen,
                    faceGrid = faceGrid,
                    zf = zf,
                    unit = unit,
                    totalYaw = totalYaw,
                    totalPitch = totalPitch,
                    rMatrix = rMatrix
                )
            } else if (frontZ < -0.005f) {
                draw3DBackPlate(
                    shape3D = shape3D,
                    project = project,
                    shellMat = shellMat,
                    zf = zf,
                    rMatrix = rMatrix
                )
            }

            // 5. Draw Ear in front
            if (earLz >= 0f) drawMesh(meshes.earL, trimMat, project, rMatrix, unit)
            if (earRz >= 0f) drawMesh(meshes.earR, trimMat, project, rMatrix, unit)

            // 6. Draw Antenna & Collar in front
            if (!antennaBehind) {
                drawMesh(meshes.collar, trimMat, project, rMatrix, unit)
                if (antenna) {
                    drawAntennaStemAndBulb(
                        shape3D = shape3D,
                        project = project,
                        trimColor = colors.trim,
                        lightConfig = lightConfig,
                        unit = unit,
                        time = time,
                        userYaw = userYaw,
                        userPitch = userPitch
                    )
                }
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
        if (rotatedNz < -0.03f) continue

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
            strokeWidth = max(0.8f, unit / 250f)
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

/**
 * 3D Front Face Plate & Screen.
 * Uses native 2D affine matrix projection with studio dome radiance gradients so the plate
 * reflects the environment and lighting smoothly, while the bezel and 22x14 LED matrix
 * seamlessly follow the 3D model orientation (Yaw, Pitch, Roll) and zoom.
 */
private fun DrawScope.draw3DFrontPlateAndFace(
    shape3D: Shape3D,
    project: (Float, Float, Float) -> FloatArray,
    shellMat: Material3D,
    screenColor: Color,
    faceGrid: Robot3DFaces.FaceGrid,
    zf: Float,
    unit: Float,
    totalYaw: Float,
    totalPitch: Float,
    rMatrix: FloatArray
) {
    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas

        val o = project(0f, 0f, zf)
        val ex = project(1f, 0f, zf)
        val ey = project(0f, 1f, zf)

        val planeMatrix = android.graphics.Matrix().apply {
            setValues(
                floatArrayOf(
                    ex[0] - o[0], ey[0] - o[0], o[0],
                    ex[1] - o[1], ey[1] - o[1], o[1],
                    0f, 0f, 1f
                )
            )
        }

        nativeCanvas.save()
        nativeCanvas.concat(planeMatrix)

        // 1. Front Shell Plate with Studio Dome Radiance Gradients (Smooth Metallic Reflections)
        val platePath = android.graphics.Path()
        shape3D.tracePath(platePath, Robot3DGeometry.InsetConstants.plate - 0.002f)

        val dome = Robot3DLight.domeColors(shellMat, rMatrix, facing = 1f, dome = 0.17f)
        val topCol = dome[0].toArgb()
        val centerCol = dome[1].toArgb()
        val bottomCol = dome[2].toArgb()
        val leftCol = dome[3].toArgb()
        val rightCol = dome[4].toArgb()

        // Vertical dome gradient
        val vShader = android.graphics.LinearGradient(
            0f, -shape3D.hy, 0f, shape3D.hy,
            intArrayOf(topCol, centerCol, bottomCol),
            floatArrayOf(0f, 0.5f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        val platePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            shader = vShader
        }
        nativeCanvas.drawPath(platePath, platePaint)

        // Horizontal cross-dome reflection overlay (50% blend for curved surface appearance)
        val hShader = android.graphics.LinearGradient(
            -shape3D.hx, 0f, shape3D.hx, 0f,
            intArrayOf(leftCol, centerCol, rightCol),
            floatArrayOf(0f, 0.5f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        val hPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            shader = hShader
            alpha = 135
        }
        nativeCanvas.drawPath(platePath, hPaint)

        // 2. Recessed Screen Hole Border (Dark Bezel Ring)
        val holePath = android.graphics.Path()
        shape3D.tracePath(holePath, Robot3DGeometry.InsetConstants.hole)
        val holePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = android.graphics.Color.rgb(7, 9, 13)
        }
        nativeCanvas.drawPath(holePath, holePaint)

        // 3. Screen Glass Area (Clipped to glass boundary)
        val glassPath = android.graphics.Path()
        shape3D.tracePath(glassPath, Robot3DGeometry.InsetConstants.glass)

        nativeCanvas.save()
        nativeCanvas.clipPath(glassPath)

        // Dark LCD Glass Background
        val glassBgPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = android.graphics.Color.rgb(12, 16, 24)
        }
        nativeCanvas.drawRect(
            RectF(-shape3D.hx, -shape3D.hy, shape3D.hx, shape3D.hy),
            glassBgPaint
        )

        // 4. 22x14 LED Matrix Face (Drawn in exact 3D model space)
        val cols = Robot3DFaces.COLS
        val rows = Robot3DFaces.ROWS
        val pitch = shape3D.pitch

        val totalW = cols * pitch
        val totalH = rows * pitch
        val startU = -totalW / 2f + pitch / 2f
        val startV = -totalH / 2f + pitch / 2f

        val cell = pitch * 0.8f
        val radius = cell / 2f

        val activeArgb = screenColor.toArgb()
        val rC = android.graphics.Color.red(activeArgb)
        val gC = android.graphics.Color.green(activeArgb)
        val bC = android.graphics.Color.blue(activeArgb)

        val ledPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        for (r in 0 until rows) {
            val v = startV + r * pitch
            for (c in 0 until cols) {
                val u = startU + c * pitch
                val intensity = faceGrid.get(c, r)

                if (intensity > 0.01f) {
                    // Outer glow bloom
                    ledPaint.color = android.graphics.Color.argb(
                        (intensity * 110).roundToInt().coerceIn(0, 255),
                        rC, gC, bC
                    )
                    nativeCanvas.drawCircle(u, v, radius * 1.85f, ledPaint)

                    // Core bright LED
                    val coreR = min(255, (rC * 0.7f + 255 * 0.3f).roundToInt())
                    val coreG = min(255, (gC * 0.7f + 255 * 0.3f).roundToInt())
                    val coreB = min(255, (bC * 0.7f + 255 * 0.3f).roundToInt())
                    ledPaint.color = android.graphics.Color.argb(
                        (intensity * 255).roundToInt().coerceIn(0, 255),
                        coreR, coreG, coreB
                    )
                    nativeCanvas.drawCircle(u, v, radius, ledPaint)
                } else {
                    // Dim unlit LED lens
                    ledPaint.color = android.graphics.Color.argb(38, 70, 76, 88)
                    nativeCanvas.drawCircle(u, v, radius * 0.72f, ledPaint)
                }
            }
        }

        // 5. Dynamic Glass Sheen Reflection (Slides as robot turns)
        val sheenShift = -0.05f - totalYaw * 0.85f + totalPitch * 0.45f
        val sheenShader = android.graphics.LinearGradient(
            -0.55f + sheenShift, -0.35f, 0.15f + sheenShift, 0.35f,
            intArrayOf(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.argb(25, 220, 235, 255),
                android.graphics.Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.5f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        val sheenPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            shader = sheenShader
        }
        nativeCanvas.drawRect(
            RectF(-shape3D.hx, -shape3D.hy, shape3D.hx, shape3D.hy),
            sheenPaint
        )

        nativeCanvas.restore() // Restore glass clip
        nativeCanvas.restore() // Restore front plane matrix
    }
}

/**
 * 3D Back Plate with cooling vent slots and dome lighting.
 */
private fun DrawScope.draw3DBackPlate(
    shape3D: Shape3D,
    project: (Float, Float, Float) -> FloatArray,
    shellMat: Material3D,
    zf: Float,
    rMatrix: FloatArray
) {
    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas

        val oBack = project(0f, 0f, -zf)
        val exBack = project(-1f, 0f, -zf) // Horizontal flip for back
        val eyBack = project(0f, 1f, -zf)

        val backMatrix = android.graphics.Matrix().apply {
            setValues(
                floatArrayOf(
                    exBack[0] - oBack[0], eyBack[0] - oBack[0], oBack[0],
                    exBack[1] - oBack[1], eyBack[1] - oBack[1], oBack[1],
                    0f, 0f, 1f
                )
            )
        }

        nativeCanvas.save()
        nativeCanvas.concat(backMatrix)

        // Back shell plate with dome lighting
        val backPlatePath = android.graphics.Path()
        shape3D.tracePath(backPlatePath, Robot3DGeometry.InsetConstants.plate - 0.002f)

        val dome = Robot3DLight.domeColors(shellMat, rMatrix, facing = -1f, dome = 0.17f)
        val vShader = android.graphics.LinearGradient(
            0f, -shape3D.hy, 0f, shape3D.hy,
            intArrayOf(dome[0].toArgb(), dome[1].toArgb(), dome[2].toArgb()),
            floatArrayOf(0f, 0.5f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        val backPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            shader = vShader
        }
        nativeCanvas.drawPath(backPlatePath, backPaint)

        // Cooling vents
        val ventPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = android.graphics.Color.rgb(18, 22, 30)
        }
        val vents = listOf(-0.12f, -0.065f, -0.01f, 0.045f, 0.1f)
        for (vy in vents) {
            val ventRect = RectF(-0.2f, vy - 0.014f, 0.2f, vy + 0.014f)
            nativeCanvas.drawRoundRect(ventRect, 0.014f, 0.014f, ventPaint)
        }

        nativeCanvas.restore()
    }
}

/**
 * True 3D Antenna Stem and Frosted Glass Sphere Bulb with dynamic emotion light & glow.
 */
private fun DrawScope.drawAntennaStemAndBulb(
    shape3D: Shape3D,
    project: (Float, Float, Float) -> FloatArray,
    trimColor: Color,
    lightConfig: Robot3DFaces.StateLightConfig,
    unit: Float,
    time: Float,
    userYaw: Float,
    userPitch: Float
) {
    val top = -shape3D.hy - 0.028f
    val stemLength = Robot3DGeometry.HeadConstants.stem

    // Natural antenna spring sway
    val ax = sin(time * 3.2f) * 0.12f - userYaw * 0.15f
    val az = cos(time * 2.6f) * 0.1f - userPitch * 0.15f
    val dir = floatArrayOf(
        sin(ax),
        -cos(ax) * cos(az),
        sin(az)
    )

    val baseP = project(0f, top, 0f)
    val ctrlP = project(0f, top - stemLength * 0.55f, 0f)
    val tipP = project(dir[0] * stemLength, top + dir[1] * stemLength, dir[2] * stemLength)
    val ballRadius = Robot3DGeometry.HeadConstants.ball
    val ballCP = project(
        dir[0] * (stemLength + ballRadius * 0.85f),
        top + dir[1] * (stemLength + ballRadius * 0.85f),
        dir[2] * (stemLength + ballRadius * 0.85f)
    )

    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas

        // 1. Antenna Stem: Multi-layer metallic 3D curve
        val stemPath = android.graphics.Path().apply {
            moveTo(baseP[0], baseP[1])
            quadTo(ctrlP[0], ctrlP[1], tipP[0], tipP[1])
        }

        val stemCorePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = max(2.5f, unit * 0.02f)
            color = android.graphics.Color.rgb(65, 75, 90)
        }
        nativeCanvas.drawPath(stemPath, stemCorePaint)

        val trimArgb = trimColor.toArgb()
        val stemTrimPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = max(1.5f, unit * 0.011f)
            color = trimArgb
        }
        nativeCanvas.drawPath(stemPath, stemTrimPaint)

        val stemHighlightPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = max(0.8f, unit * 0.005f)
            color = android.graphics.Color.argb(190, 255, 255, 255)
        }
        nativeCanvas.drawPath(stemPath, stemHighlightPaint)

        // 2. Antenna Bulb: 3D Frosted Glass Sphere
        val bx = ballCP[0]
        val by = ballCP[1]
        val r = ballRadius * unit

        val level = lightConfig.pulse(time).coerceIn(0f, 1f)
        val ballColorArgb = lightConfig.ballColor.toArgb()
        val bR = android.graphics.Color.red(ballColorArgb)
        val bG = android.graphics.Color.green(ballColorArgb)
        val bB = android.graphics.Color.blue(ballColorArgb)

        // Outer glow halo
        if (level > 0.05f) {
            val glowRadius = r * 3.8f
            val glowShader = android.graphics.RadialGradient(
                bx, by, glowRadius,
                intArrayOf(
                    android.graphics.Color.argb((level * 115).roundToInt(), bR, bG, bB),
                    android.graphics.Color.argb((level * 35).roundToInt(), bR, bG, bB),
                    android.graphics.Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.4f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
            val glowPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                shader = glowShader
            }
            nativeCanvas.drawCircle(bx, by, glowRadius, glowPaint)
        }

        // Frosted glass sphere with internal filament
        val coreLitR = min(255, (bR * 0.5f + 255 * 0.5f).roundToInt())
        val coreLitG = min(255, (bG * 0.5f + 255 * 0.5f).roundToInt())
        val coreLitB = min(255, (bB * 0.5f + 255 * 0.5f).roundToInt())

        val bulbShader = android.graphics.RadialGradient(
            bx - r * 0.22f, by - r * 0.25f, r * 1.05f,
            intArrayOf(
                android.graphics.Color.rgb(coreLitR, coreLitG, coreLitB),
                android.graphics.Color.rgb(bR, bG, bB),
                android.graphics.Color.rgb(
                    (bR * 0.6f + 70 * 0.4f).roundToInt(),
                    (bG * 0.6f + 76 * 0.4f).roundToInt(),
                    (bB * 0.6f + 88 * 0.4f).roundToInt()
                )
            ),
            floatArrayOf(0f, 0.55f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )

        val bulbPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            shader = bulbShader
        }
        nativeCanvas.drawCircle(bx, by, r, bulbPaint)

        // Silhouette rim
        val rimPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = max(0.8f, r * 0.08f)
            color = android.graphics.Color.argb(85, 25, 30, 40)
        }
        nativeCanvas.drawCircle(bx, by, r, rimPaint)

        // White specular glint reflection spot
        val glintPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = android.graphics.Color.argb(220, 255, 255, 255)
        }
        nativeCanvas.drawCircle(bx - r * 0.35f, by - r * 0.38f, r * 0.26f, glintPaint)
    }
}
