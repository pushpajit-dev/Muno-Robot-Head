package com.munorobo.app.model

import androidx.compose.ui.graphics.Color
import kotlin.math.*

data class Material3D(
    val albedo: FloatArray,     // linear RGB [r, g, b]
    val f0: Float = 0.05f,       // clear-coat reflectance
    val metal: Float = 0.15f,    // metalness tint
    val spec: Float = 0.55f,     // specular strength
    val shininess: Float = 70f,  // specular exponent
    val rim: Float = 0.9f,       // rim light strength
    val isMetallic: Boolean = false,
    val isHolo: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Material3D) return false
        return albedo.contentEquals(other.albedo) &&
                f0 == other.f0 && metal == other.metal &&
                spec == other.spec && shininess == other.shininess &&
                rim == other.rim && isMetallic == other.isMetallic && isHolo == other.isHolo
    }

    override fun hashCode(): Int {
        var result = albedo.contentHashCode()
        result = 31 * result + f0.hashCode()
        result = 31 * result + metal.hashCode()
        result = 31 * result + spec.hashCode()
        result = 31 * result + shininess.hashCode()
        result = 31 * result + rim.hashCode()
        result = 31 * result + isMetallic.hashCode()
        result = 31 * result + isHolo.hashCode()
        return result
    }
}

object Robot3DLight {
    private fun n3(x: Float, y: Float, z: Float): FloatArray {
        val l = hypot(hypot(x, y), z).coerceAtLeast(1e-6f)
        return floatArrayOf(x / l, y / l, z / l)
    }

    val KEY: FloatArray = n3(-0.5f, -0.78f, 0.62f)
    private val FILL: FloatArray = n3(0.85f, 0.15f, 0.5f)
    private val BACK: FloatArray = n3(0.55f, -0.55f, -0.62f)
    private val HALF: FloatArray = n3(KEY[0], KEY[1], KEY[2] + 1f)

    private fun smooth(a: Float, b: Float, x: Float): Float {
        val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun environment(rx: Float, ry: Float, rz: Float): Float {
        val up = -ry
        val horizon = 0.05f + 0.12f * smooth(-0.25f, 0.3f, up)
        val softbox = 1.1f * smooth(0.42f, 0.78f, up) * smooth(-0.9f, -0.2f, rz - 0.6f * abs(rx))
        val window = 2.6f * smooth(0.86f, 0.97f, rx * KEY[0] + ry * KEY[1] + rz * KEY[2])
        val strip = 0.5f * smooth(0.6f, 0.85f, -rx) * smooth(-0.35f, 0.05f, up) * (1f - smooth(0.2f, 0.5f, up))
        return horizon + softbox + window + strip
    }

    private fun toLinear(c: Float): Float = (c / 255f).pow(2.2f)

    fun colorToLinear(color: Color): FloatArray {
        return floatArrayOf(
            toLinear(color.red * 255f),
            toLinear(color.green * 255f),
            toLinear(color.blue * 255f)
        )
    }

    private fun display(x: Float): Int {
        val t = (x * (2.51f * x + 0.03f)) / (x * (2.43f * x + 0.59f) + 0.14f)
        val srgb = t.coerceIn(0f, 1f).pow(1f / 2.2f)
        return (srgb * 255f).roundToInt().coerceIn(0, 255)
    }

    fun shade(m: Material3D, nx: Float, ny: Float, nz: Float): Color {
        val ndl = nx * KEY[0] + ny * KEY[1] + nz * KEY[2]
        val key = max(0f, (ndl + 0.12f) / 1.12f)
        val fill = max(0f, nx * FILL[0] + ny * FILL[1] + nz * FILL[2])
        val sky = 0.5f - 0.5f * ny
        val ndv = max(0f, nz)
        val fres = m.f0 + (1f - m.f0) * (1f - ndv).pow(5f)

        val rx = 2f * ndv * nx
        val ry = 2f * ndv * ny
        val rz = 2f * ndv * nz - 1f
        val env = environment(rx, ry, rz) * fres

        val specAngle = max(0f, nx * HALF[0] + ny * HALF[1] + nz * HALF[2])
        val spec = m.spec * specAngle.pow(m.shininess)

        val backAngle = max(0f, nx * BACK[0] + ny * BACK[1] + nz * BACK[2])
        val rim = m.rim * backAngle * backAngle * (1f - ndv).pow(1.6f)

        // For metals, diffuse is attenuated in favor of specular and environment reflections
        val diffuseWeight = (1f - m.metal * 0.72f).coerceIn(0.12f, 1f)
        val diffuse = (0.05f + 0.1f * sky + 1.25f * key + 0.16f * fill) * diffuseWeight

        var r = computeChannel(0, m, diffuse, fres, env, spec, rim)
        var g = computeChannel(1, m, diffuse, fres, env, spec, rim)
        var b = computeChannel(2, m, diffuse, fres, env, spec, rim)

        // Iridescent pearlescent rainbow sheen for HOLO texture
        if (m.isHolo) {
            val holoPhase = (1f - ndv) * 2.8f + (nx * 0.4f + ny * 0.4f)
            val hR = (0.5f + 0.5f * sin(holoPhase * 6.28f)) * fres * 0.65f
            val hG = (0.5f + 0.5f * sin((holoPhase + 0.33f) * 6.28f)) * fres * 0.65f
            val hB = (0.5f + 0.5f * sin((holoPhase + 0.67f) * 6.28f)) * fres * 0.65f
            r += hR
            g += hG
            b += hB
        }

        return Color(display(r), display(g), display(b))
    }

    private fun computeChannel(
        i: Int,
        m: Material3D,
        diffuse: Float,
        fres: Float,
        env: Float,
        spec: Float,
        rim: Float
    ): Float {
        val a = m.albedo[i]
        val tint = 1f - m.metal + m.metal * a * 2.5f
        val rimTint = if (i == 0) 0.62f else if (i == 1) 0.78f else 1f
        return a * diffuse * (1f - fres * 0.55f) + (env + spec) * tint + rim * rimTint * (0.35f + a)
    }

    /**
     * Helper to compute the curved dome radiance colors for the front and back face plates.
     */
    fun domeColors(
        material: Material3D,
        rMatrix: FloatArray,
        facing: Float = 1f,
        dome: Float = 0.17f
    ): List<Color> {
        val at = { dx: Float, dy: Float ->
            val w = Robot3DGeometry.rotate(rMatrix, dx * (if (facing < 0f) -1f else 1f), dy, facing)
            val l = hypot(hypot(w[0], w[1]), w[2]).coerceAtLeast(1e-6f)
            shade(material, w[0] / l, w[1] / l, w[2] / l)
        }

        val top = at(0f, -dome)
        val center = at(0f, 0f)
        val bottom = at(0f, dome)
        val left = at(-dome, 0f)
        val right = at(dome, 0f)

        return listOf(top, center, bottom, left, right)
    }
}
