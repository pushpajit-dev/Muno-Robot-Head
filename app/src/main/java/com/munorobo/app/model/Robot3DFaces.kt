package com.munorobo.app.model

import androidx.compose.ui.graphics.Color
import kotlin.math.*

object Robot3DFaces {
    const val COLS = 22
    const val ROWS = 14

    const val EYE_L = 6
    const val EYE_R = 13
    const val EYE_TOP = 5

    class FaceGrid(val cols: Int = COLS, val rows: Int = ROWS) {
        val values = FloatArray(cols * rows)

        fun clear() {
            values.fill(0f)
        }

        fun put(c: Int, r: Int, v: Float) {
            if (c in 0 until cols && r in 0 until rows) {
                val idx = r * cols + c
                if (v > values[idx]) {
                    values[idx] = v.coerceIn(0f, 1f)
                }
            }
        }

        fun rect(c: Int, r: Int, w: Int, h: Int, v: Float) {
            for (y in r until r + h) {
                for (x in c until c + w) {
                    put(x, y, v)
                }
            }
        }

        fun sprite(c: Int, r: Int, rowsData: List<String>, v: Float) {
            rowsData.forEachIndexed { y, row ->
                for (x in row.indices) {
                    when (row[x]) {
                        '#' -> put(c + x, r + y, v)
                        '+' -> put(c + x, r + y, v * 0.5f)
                    }
                }
            }
        }

        fun get(c: Int, r: Int): Float {
            return if (c in 0 until cols && r in 0 until rows) values[r * cols + c] else 0f
        }
    }

    private fun noise1D(x: Float): Float {
        return (0.5f + 0.25f * sin(x * 1.7f) + 0.15f * sin(x * 3.1f + 1.3f) + 0.1f * sin(x * 5.3f + 4.1f)).coerceIn(0f, 1f)
    }

    private fun eyes(
        grid: FaceGrid,
        lookX: Float = 0f,
        lookY: Float = 0f,
        blink: Float = 0f,
        h: Int = 4,
        v: Float = 1f,
        dy: Int = 0,
        lid: Float = 0f
    ) {
        val dx = lookX.roundToInt()
        val ly = lookY.roundToInt() + dy
        val open = max(1, (h - (h - 1) * blink).roundToInt())
        val top = EYE_TOP + (4 - h) / 2 + ly + (h - open) / 2

        for (c in listOf(EYE_L, EYE_R)) {
            if (lid > 0f && open > 1) {
                grid.rect(c + dx, top, 3, 1, v * (1f - lid))
                grid.rect(c + dx, top + 1, 3, open - 1, v)
            } else {
                grid.rect(c + dx, top, 3, open, v)
            }
        }
    }

    private val CARET = listOf("..#..", ".#.#.", "#...#")
    private val CROSS = listOf("#...#", ".#.#.", "..#..", ".#.#.", "#...#")
    private val Z_BIG = listOf("####", "..#.", ".#..", "####")
    private val Z_SMALL = listOf("###", ".#.", "###")

    fun populate(grid: FaceGrid, state: RobotHeadState, time: Float) {
        grid.clear()

        // Natural periodic blink calculation
        val blinkCycle = (time % 3.5f)
        val blink = if (blinkCycle in 2.8f..3.0f) {
            val p = (blinkCycle - 2.8f) / 0.2f
            sin(p * PI.toFloat())
        } else 0f

        when (state) {
            RobotHeadState.IDLE -> {
                val lookX = sin(time * 0.7f) * 1.2f
                val lookY = cos(time * 0.5f) * 0.6f
                eyes(grid, lookX = lookX, lookY = lookY, blink = blink)
            }

            RobotHeadState.THINKING -> {
                val side = if (sin(time * 0.8f) > 0) 1f else -1f
                eyes(grid, lookX = side * 1.4f, lookY = -1.2f, blink = blink, h = 3, lid = 0.45f)
                // 3 pulsing dots at bottom
                for (i in 0 until 3) {
                    val p = (time * 1.6f - i * 0.22f) % 1f
                    val pulse = if (p in 0f..0.35f) sin((p / 0.35f) * PI.toFloat()) else 0f
                    grid.rect(6 + i * 4, 11 - (if (pulse > 0.5f) 1 else 0), 2, 2, 0.28f + 0.72f * pulse)
                }
            }

            RobotHeadState.SEARCHING -> {
                eyes(grid, blink = blink)
                // Sweeping radar beam with trail
                val sweep = (sin(time * 1.6f + 0.4f) * 0.5f + 0.5f) * (COLS - 1)
                val dir = if (cos(time * 1.6f + 0.4f) > 0) -1 else 1
                for (k in 0 until 5) {
                    val c = (sweep + dir * k).roundToInt()
                    val v = 0.35f * (1f - k / 5f)
                    for (r in 0 until ROWS) {
                        grid.put(c, r, v)
                    }
                }
            }

            RobotHeadState.LISTENING -> {
                eyes(grid, blink = blink, h = 4, dy = -1)
                // Equalizer bouncing to audio noise
                for (i in 0 until 12) {
                    val center = 1f - abs(i - 5.5f) / 7f
                    val level = noise1D(time * 7f + i * 1.9f) * center
                    val h = max(1, (level * 4.2f).roundToInt())
                    for (k in 0 until h) {
                        grid.put(5 + i, 12 - k, 0.85f - k * 0.12f)
                    }
                }
            }

            RobotHeadState.SPEAKING -> {
                eyes(grid, blink = blink, h = 4, dy = -1)
                // Speech mouth opening and closing
                val env = max(0f, sin(time * 2.1f) * 0.5f + sin(time * 3.7f + 1f) * 0.5f)
                for (i in 0 until 8) {
                    val center = 1f - abs(i - 3.5f) / 5f
                    val a = noise1D(time * 11f + i * 0.9f) * center * (0.35f + env)
                    val h = if (a > 0.55f) 3 else if (a > 0.28f) 2 else 1
                    val top = if (h == 3) 10 else 11
                    grid.rect(7 + i, top, 1, h, 1f)
                }
            }

            RobotHeadState.WORKING -> {
                // Focused flat eyes looking down
                eyes(grid, lookY = 1f, h = 2, blink = 0f)
                // Progress bar
                val p = (time / 2.6f) % 1f
                val fill = (p * 12).roundToInt()
                for (i in 0 until 12) {
                    grid.put(5 + i, 11, if (i < fill) 0.95f else 0.16f)
                }
                grid.put(4, 11, 0.4f)
                grid.put(17, 11, 0.4f)
            }

            RobotHeadState.HAPPY -> {
                val hop = max(0f, sin(time * 5.6f) * -1f).roundToInt()
                grid.sprite(EYE_L - 1, EYE_TOP + hop, CARET, 1f)
                grid.sprite(EYE_R - 1, EYE_TOP + hop, CARET, 1f)
                // Wide smile
                grid.sprite(7, 10 + hop, listOf("#......#", ".######."), 0.95f)
            }

            RobotHeadState.ERROR -> {
                val on = if (sin(time * 7f) > -0.6f) 1f else 0.55f
                grid.sprite(EYE_L - 1, 4, CROSS, on)
                grid.sprite(EYE_R - 1, 4, CROSS, on)
                for (i in 0 until 8) {
                    grid.put(7 + i, if (i % 2 == 1) 11 else 12, 0.85f)
                }
            }

            RobotHeadState.SLEEPING -> {
                // Shut curved eyes
                for (c in listOf(EYE_L, EYE_R)) {
                    grid.put(c - 1, 7, 0.35f)
                    grid.rect(c, 8, 3, 1, 0.45f)
                    grid.put(c + 3, 7, 0.35f)
                }
                // Floating Zz
                listOf(Pair(0f, true), Pair(1.1f, false)).forEach { (delay, big) ->
                    val p = ((time + delay) / 2.2f) % 1f
                    val v = sin(p * PI.toFloat()) * 0.7f
                    val y = (5 - p * 5).roundToInt()
                    val x = (16 + p * 2).roundToInt()
                    grid.sprite(x, y, if (big) Z_BIG else Z_SMALL, v)
                }
            }
        }
    }

    data class StateLightConfig(
        val screenColorOverride: Color? = null,
        val ballColor: Color,
        val pulse: (Float) -> Float
    )

    fun getLightConfig(state: RobotHeadState): StateLightConfig {
        return when (state) {
            RobotHeadState.IDLE -> StateLightConfig(
                screenColorOverride = null,
                ballColor = Color(0xFFFFFAEF),
                pulse = { t -> 0.75f + 0.1f * sin(t * 1.4f) }
            )
            RobotHeadState.THINKING -> StateLightConfig(
                screenColorOverride = null,
                ballColor = Color(0xFFFFB840),
                pulse = { t -> 0.45f + 0.55f * (0.5f + 0.5f * sin(t * 4.2f)) }
            )
            RobotHeadState.SEARCHING -> StateLightConfig(
                screenColorOverride = null,
                ballColor = Color(0xFF5ADCFF),
                pulse = { t -> if ((t * 2.4f) % 1f < 0.18f) 1f else 0.25f }
            )
            RobotHeadState.LISTENING -> StateLightConfig(
                screenColorOverride = null,
                ballColor = Color(0xFF60FF96),
                pulse = { 0.95f }
            )
            RobotHeadState.SPEAKING -> StateLightConfig(
                screenColorOverride = null,
                ballColor = Color(0xFFFFFAEF),
                pulse = { t -> 0.55f + 0.45f * noise1D(t * 11f) }
            )
            RobotHeadState.WORKING -> StateLightConfig(
                screenColorOverride = null,
                ballColor = Color(0xFF6EA0FF),
                pulse = { t -> if ((t * 1.7f) % 1f < 0.5f) 1f else 0.35f }
            )
            RobotHeadState.HAPPY -> StateLightConfig(
                screenColorOverride = null,
                ballColor = Color(0xFFFF82C8),
                pulse = { t -> 0.8f + 0.2f * sin(t * 8f) }
            )
            RobotHeadState.ERROR -> StateLightConfig(
                screenColorOverride = Color(0xFFFF4640),
                ballColor = Color(0xFFFF3C32),
                pulse = { t -> if ((t * 3f) % 1f < 0.5f) 1f else 0.15f }
            )
            RobotHeadState.SLEEPING -> StateLightConfig(
                screenColorOverride = null,
                ballColor = Color(0xFFFFFAEF),
                pulse = { 0f }
            )
        }
    }
}
