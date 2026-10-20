package com.munorobo.app.model

import android.graphics.Path
import kotlin.math.*

data class Mesh(
    val pos: FloatArray,            // [x, y, z] per vertex
    val quads: IntArray,            // 4 indices per quad
    val normals: FloatArray,         // [nx, ny, nz] per quad
    val vertexNormals: FloatArray   // [nx, ny, nz] per vertex
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Mesh) return false
        return pos.contentEquals(other.pos) &&
                quads.contentEquals(other.quads) &&
                normals.contentEquals(other.normals) &&
                vertexNormals.contentEquals(other.vertexNormals)
    }

    override fun hashCode(): Int {
        var result = pos.contentHashCode()
        result = 31 * result + quads.contentHashCode()
        result = 31 * result + normals.contentHashCode()
        result = 31 * result + vertexNormals.contentHashCode()
        return result
    }
}

data class Corner3D(val x: Float, val y: Float, val a0: Float, val a1: Float)

class Shape3D(
    val name: RobotHeadShape,
    val r: Float,
    val depth: Float,
    val corners: List<Corner3D>,
    val hx: Float,
    val hy: Float,
    val pitch: Float,
    val earX: Float,
    val screws: List<Triple<Float, Float, Float>>,
    val sdPoly: (Float, Float) -> Float
) {
    fun sd(x: Float, y: Float, inset: Float): Float = sdPoly(x, y) - (r - inset)

    fun tracePath(path: Path, inset: Float, scale: Float = 1f, offsetX: Float = 0f, offsetY: Float = 0f) {
        val rr = max(0.0005f, r - inset) * scale
        corners.forEachIndexed { i, c ->
            val cx = c.x * scale + offsetX
            val cy = c.y * scale + offsetY
            val startX = cx + rr * cos(c.a0)
            val startY = cy + rr * sin(c.a0)
            if (i == 0) {
                path.moveTo(startX, startY)
            } else {
                path.lineTo(startX, startY)
            }
            val sweep = c.a1 - c.a0
            val steps = max(3, ceil((sweep / (PI.toFloat() / 2f)) * 14f).toInt())
            for (s in 1..steps) {
                val a = c.a0 + (s.toFloat() / steps) * sweep
                path.lineTo(cx + rr * cos(a), cy + rr * sin(a))
            }
        }
        path.close()
    }
}

object Robot3DGeometry {
    const val TAU = (PI * 2.0).toFloat()

    object HeadConstants {
        const val bevel = 0.075f
        const val margin = 0.045f
        const val gasket = 0.022f
        const val recess = 0.03f
        const val earY = 0.03f
        const val stem = 0.2f
        const val ball = 0.052f
    }

    object InsetConstants {
        const val plate = HeadConstants.bevel
        const val hole = HeadConstants.bevel + HeadConstants.margin
        const val glass = HeadConstants.bevel + HeadConstants.margin + HeadConstants.gasket
    }

    fun rotation(yaw: Float, pitch: Float, roll: Float): FloatArray {
        val cy = cos(yaw)
        val sy = sin(yaw)
        val cp = cos(pitch)
        val sp = sin(pitch)
        val cr = cos(roll)
        val sr = sin(roll)

        // Yaw * Pitch
        val a0 = cy
        val a1 = sy * sp
        val a2 = sy * cp
        val a3 = 0f
        val a4 = cp
        val a5 = -sp
        val a6 = -sy
        val a7 = cy * sp
        val a8 = cy * cp

        // Roll * that
        return floatArrayOf(
            cr * a0 - sr * a3, cr * a1 - sr * a4, cr * a2 - sr * a5,
            sr * a0 + cr * a3, sr * a1 + cr * a4, sr * a2 + cr * a5,
            a6, a7, a8
        )
    }

    fun rotate(m: FloatArray, x: Float, y: Float, z: Float): FloatArray {
        return floatArrayOf(
            m[0] * x + m[1] * y + m[2] * z,
            m[3] * x + m[4] * y + m[5] * z,
            m[6] * x + m[7] * y + m[8] * z
        )
    }

    private fun norm(x: Float, y: Float, z: Float): FloatArray {
        val l = hypot(hypot(x, y), z).coerceAtLeast(1e-6f)
        return floatArrayOf(x / l, y / l, z / l)
    }

    private fun stitch(rings: Int, samples: Int, closed: Boolean = true): IntArray {
        val quads = ArrayList<Int>()
        val span = if (closed) samples else samples - 1
        for (k in 0 until rings - 1) {
            for (j in 0 until span) {
                val j2 = (j + 1) % samples
                quads.add(k * samples + j)
                quads.add(k * samples + j2)
                quads.add((k + 1) * samples + j2)
                quads.add((k + 1) * samples + j)
            }
        }
        return quads.toIntArray()
    }

    private fun finish(pos: FloatArray, quads: IntArray, vn: FloatArray): Mesh {
        val numQuads = quads.size / 4
        val normals = FloatArray(numQuads * 3)
        for (q in 0 until numQuads) {
            var x = 0f
            var y = 0f
            var z = 0f
            for (j in 0 until 4) {
                val i = quads[q * 4 + j] * 3
                x += vn[i]
                y += vn[i + 1]
                z += vn[i + 2]
            }
            val n = norm(x, y, z)
            normals[q * 3] = n[0]
            normals[q * 3 + 1] = n[1]
            normals[q * 3 + 2] = n[2]
        }
        return Mesh(pos, quads, normals, vn)
    }

    private fun makeShape(
        name: RobotHeadShape,
        centres: List<Pair<Float, Float>>,
        r: Float,
        depth: Float,
        screwDirs: List<Float> = listOf(45f, 135f)
    ): Shape3D {
        val sorted = centres.map { (x, y) -> Triple(x, y, atan2(y, x)) }
            .sortedBy { it.third }
        val n = sorted.size

        val edge = ArrayList<Float>()
        for (i in 0 until n) {
            val a = sorted[i]
            val b = sorted[(i + 1) % n]
            edge.add(atan2(-(b.first - a.first), b.second - a.second))
        }

        val corners = sorted.mapIndexed { i, p ->
            if (n == 1) {
                Corner3D(p.first, p.second, -PI.toFloat(), PI.toFloat())
            } else {
                var a0 = edge[(i - 1 + n) % n]
                var a1 = edge[i]
                while (a1 < a0) a1 += TAU
                Corner3D(p.first, p.second, a0, a1)
            }
        }

        val sdPoly: (Float, Float) -> Float = { x, y ->
            if (n == 1) {
                hypot(x - sorted[0].first, y - sorted[0].second)
            } else {
                var d = Float.POSITIVE_INFINITY
                var inside = true
                for (i in 0 until n) {
                    val a = sorted[i]
                    val b = sorted[(i + 1) % n]
                    val ex = b.first - a.first
                    val ey = b.second - a.second
                    val px = x - a.first
                    val py = y - a.second
                    val t = ((px * ex + py * ey) / (ex * ex + ey * ey)).coerceIn(0f, 1f)
                    d = min(d, hypot(px - ex * t, py - ey * t))
                    if (ex * py - ey * px < 0) inside = false
                }
                if (inside) -d else d
            }
        }

        var hx = 0f
        var hy = 0f
        for (p in sorted) {
            hx = max(hx, abs(p.first) + r)
            hy = max(hy, abs(p.second) + r)
        }

        var lo = 0f
        var hi = hx + 0.1f
        for (i in 0 until 40) {
            val mid = (lo + hi) / 2f
            if (sdPoly(mid, HeadConstants.earY) - r < 0) lo = mid else hi = mid
        }

        val gw = 2f * (hx - InsetConstants.glass)
        val gh = 2f * (hy - InsetConstants.glass)
        val pitch = min((0.6f * gw) / 14f, (0.65f * gh) / 10f)

        // Corner screw placement
        val inward = (InsetConstants.hole - 0.018f + InsetConstants.plate) / 2f
        val screws = screwDirs.mapIndexed { idx, deg ->
            val rad = Math.toRadians(deg.toDouble()).toFloat()
            val c = corners.find { k ->
                val diff0 = (rad - k.a0).mod(TAU)
                val diff1 = (k.a1 - rad).mod(TAU)
                diff0 >= -1e-4f && diff1 >= -1e-4f
            } ?: corners[0]
            Triple(c.x + cos(rad) * (r - inward), c.y + sin(rad) * (r - inward), 0.5f + idx * 0.9f)
        }

        return Shape3D(name, r, depth, corners, hx, hy, pitch, lo - 0.008f, screws, sdPoly)
    }

    private val hexCentres = listOf(0, 60, 120, 180, 240, 300).map { d ->
        val rad = Math.toRadians(d.toDouble()).toFloat()
        Pair(0.35f * cos(rad), 0.35f * sin(rad))
    }

    fun getShape(shape: RobotHeadShape): Shape3D {
        return when (shape) {
            RobotHeadShape.RECTANGLE -> makeShape(
                RobotHeadShape.RECTANGLE,
                listOf(Pair(-0.29f, -0.17f), Pair(0.29f, -0.17f), Pair(0.29f, 0.17f), Pair(-0.29f, 0.17f)),
                0.21f,
                0.6f,
                listOf(45f, 135f)
            )
            RobotHeadShape.SQUARE -> makeShape(
                RobotHeadShape.SQUARE,
                listOf(Pair(-0.22f, -0.22f), Pair(0.22f, -0.22f), Pair(0.22f, 0.22f), Pair(-0.22f, 0.22f)),
                0.2f,
                0.6f,
                listOf(45f, 135f)
            )
            RobotHeadShape.CIRCLE -> makeShape(
                RobotHeadShape.CIRCLE,
                listOf(Pair(0f, 0f)),
                0.46f,
                0.56f,
                listOf(45f, 135f)
            )
            RobotHeadShape.HEXAGON -> makeShape(
                RobotHeadShape.HEXAGON,
                hexCentres,
                0.16f,
                0.58f,
                listOf(60f, 120f)
            )
        }
    }

    /**
     * Builds beveled body mesh with high tessellation (16 bevel steps, 16 outline steps)
     * for seamless smooth lighting without polygonal faceting.
     */
    fun buildBody(shape: Shape3D, bevelSteps: Int = 16): Mesh {
        val r = shape.r
        val b = HeadConstants.bevel
        val zf = shape.depth / 2f

        val outline = ArrayList<FloatArray>()
        for (c in shape.corners) {
            val sweep = c.a1 - c.a0
            val steps = max(4, ceil((sweep / (PI.toFloat() / 2f)) * 16f).toInt())
            val last = if (sweep >= TAU - 1e-6f) steps - 1 else steps
            for (i in 0..last) {
                val a = c.a0 + (i.toFloat() / steps) * sweep
                outline.add(floatArrayOf(c.x, c.y, cos(a), sin(a)))
            }
        }

        val profile = ArrayList<FloatArray>()
        for (i in bevelSteps downTo 0) {
            val t = (i.toFloat() / bevelSteps) * (PI.toFloat() / 2f)
            profile.add(floatArrayOf(-zf + b - b * sin(t), b * (1f - cos(t)), -t))
        }
        for (i in 0..bevelSteps) {
            val t = (i.toFloat() / bevelSteps) * (PI.toFloat() / 2f)
            profile.add(floatArrayOf(zf - b + b * sin(t), b * (1f - cos(t)), t))
        }

        val pos = ArrayList<Float>()
        val vn = ArrayList<Float>()
        for (p in profile) {
            val z = p[0]
            val inset = p[1]
            val t = p[2]
            for (o in outline) {
                val cx = o[0]
                val cy = o[1]
                val nx = o[2]
                val ny = o[3]
                pos.add(cx + (r - inset) * nx)
                pos.add(cy + (r - inset) * ny)
                pos.add(z)
                vn.add(nx * cos(t))
                vn.add(ny * cos(t))
                vn.add(sin(t))
            }
        }

        return finish(pos.toFloatArray(), stitch(profile.size, outline.size), vn.toFloatArray())
    }

    /**
     * High-resolution lathe mesh with 48 segments for perfectly curved, mirror-smooth metallic knobs.
     */
    fun buildLathe(
        profile: List<Pair<Float, Float>>,
        origin: FloatArray,
        axis: FloatArray,
        segments: Int = 48
    ): Mesh {
        val e1 = if (abs(axis[1]) > 0.9f) floatArrayOf(1f, 0f, 0f) else floatArrayOf(0f, 1f, 0f)
        val e2 = norm(
            axis[1] * e1[2] - axis[2] * e1[1],
            axis[2] * e1[0] - axis[0] * e1[2],
            axis[0] * e1[1] - axis[1] * e1[0]
        )

        val pos = ArrayList<Float>()
        val vn = ArrayList<Float>()
        for (k in profile.indices) {
            val r = profile[k].first
            val t = profile[k].second
            val prev = profile[max(0, k - 1)]
            val next = profile[min(profile.size - 1, k + 1)]
            val dr = next.first - prev.first
            val dt = next.second - prev.second
            val l = hypot(dr, dt).coerceAtLeast(1e-6f)
            val nr = dt / l
            val na = -dr / l

            for (j in 0 until segments) {
                val a = (j.toFloat() / segments) * TAU
                val rx = cos(a) * e1[0] + sin(a) * e2[0]
                val ry = cos(a) * e1[1] + sin(a) * e2[1]
                val rz = cos(a) * e1[2] + sin(a) * e2[2]

                pos.add(origin[0] + axis[0] * t + r * rx)
                pos.add(origin[1] + axis[1] * t + r * ry)
                pos.add(origin[2] + axis[2] * t + r * rz)

                vn.add(rx * nr + axis[0] * na)
                vn.add(ry * nr + axis[1] * na)
                vn.add(rz * nr + axis[2] * na)
            }
        }

        return finish(pos.toFloatArray(), stitch(profile.size, segments), vn.toFloatArray())
    }

    private val earProfile = listOf(
        Pair(0.06f, -0.012f), Pair(0.068f, 0f), Pair(0.074f, 0.006f), Pair(0.08f, 0.018f),
        Pair(0.084f, 0.032f), Pair(0.084f, 0.048f), Pair(0.079f, 0.06f), Pair(0.068f, 0.07f),
        Pair(0.052f, 0.077f), Pair(0.032f, 0.081f), Pair(0.014f, 0.083f), Pair(0f, 0.0835f)
    )

    private val collarProfile = listOf(
        Pair(0.05f, -0.012f), Pair(0.052f, 0.006f), Pair(0.05f, 0.016f),
        Pair(0.042f, 0.024f), Pair(0.026f, 0.029f), Pair(0.014f, 0.031f), Pair(0f, 0.032f)
    )

    data class HeadMeshes3D(
        val body: Mesh,
        val earL: Mesh,
        val earR: Mesh,
        val collar: Mesh
    )

    private val cache = HashMap<RobotHeadShape, HeadMeshes3D>()

    fun headMeshes(shape: Shape3D): HeadMeshes3D {
        return cache.getOrPut(shape.name) {
            HeadMeshes3D(
                body = buildBody(shape, bevelSteps = 16),
                earL = buildLathe(earProfile, floatArrayOf(-shape.earX, HeadConstants.earY, 0f), floatArrayOf(-1f, 0f, 0f), segments = 48),
                earR = buildLathe(earProfile, floatArrayOf(shape.earX, HeadConstants.earY, 0f), floatArrayOf(1f, 0f, 0f), segments = 48),
                collar = buildLathe(collarProfile, floatArrayOf(0f, -shape.hy, 0f), floatArrayOf(0f, -1f, 0f), segments = 32)
            )
        }
    }
}
