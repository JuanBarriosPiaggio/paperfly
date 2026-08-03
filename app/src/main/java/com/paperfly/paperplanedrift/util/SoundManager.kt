package com.paperfly.paperplanedrift.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Tiny procedural sound engine — no audio assets needed.
 *
 * One-shots: paper thud (crash), tick (milestone/bonus), swoosh (launch/revive),
 * snip (scissors closing), snap (stapler slamming shut).
 *
 * Loops (started while flying, mixed by volume every frame):
 * - glide: airy paper-in-wind noise, swells while diving
 * - wind:  fluttering rustle, audible inside a gust
 * - fan:   motor hum + blade whoosh, louder as a desk fan approaches
 */
class SoundManager {

    @Volatile
    private var _enabled = true
    var enabled: Boolean
        get() = _enabled
        set(value) {
            _enabled = value
            if (!value) muteLoops()
        }

    private val sampleRate = 22050

    // --- one-shots ---
    private val thudTrack: AudioTrack? by lazy { buildTrack(thudPcm()) }
    private val tickTrack: AudioTrack? by lazy { buildTrack(tickPcm()) }
    private val swooshTrack: AudioTrack? by lazy { buildTrack(swooshPcm()) }
    private val snipTrack: AudioTrack? by lazy { buildTrack(snipPcm()) }
    private val snapTrack: AudioTrack? by lazy { buildTrack(snapPcm()) }

    fun playThud() = play(thudTrack)
    fun playTick() = play(tickTrack)
    fun playSwoosh() = play(swooshTrack)
    fun playSnip() = play(snipTrack)
    fun playSnap() = play(snapTrack)

    // --- loops ---
    private val glideLoop: AudioTrack? by lazy { buildLoop(glidePcm()) }
    private val windLoop: AudioTrack? by lazy { buildLoop(windPcm()) }
    private val fanLoop: AudioTrack? by lazy { buildLoop(fanPcm()) }

    private var loopsRunning = false
    private var lastGlide = -1f
    private var lastWind = -1f
    private var lastFan = -1f

    fun startLoops() {
        if (loopsRunning) return
        loopsRunning = true
        lastGlide = -1f; lastWind = -1f; lastFan = -1f
        for (t in listOf(glideLoop, windLoop, fanLoop)) {
            runCatching {
                t?.setVolume(0f)
                t?.play()
            }
        }
    }

    fun stopLoops() {
        if (!loopsRunning) return
        loopsRunning = false
        for (t in listOf(glideLoop, windLoop, fanLoop)) {
            runCatching { t?.pause() }
        }
    }

    /** Per-frame ambient mix; levels are 0..1 and cheap to set repeatedly. */
    fun setLoopLevels(glide: Float, wind: Float, fan: Float) {
        if (!loopsRunning) return
        val g = if (_enabled) glide.coerceIn(0f, 1f) else 0f
        val w = if (_enabled) wind.coerceIn(0f, 1f) else 0f
        val f = if (_enabled) fan.coerceIn(0f, 1f) else 0f
        if (abs(g - lastGlide) > 0.02f) { runCatching { glideLoop?.setVolume(g * 0.40f) }; lastGlide = g }
        if (abs(w - lastWind) > 0.02f) { runCatching { windLoop?.setVolume(w * 0.60f) }; lastWind = w }
        if (abs(f - lastFan) > 0.02f) { runCatching { fanLoop?.setVolume(f * 0.55f) }; lastFan = f }
    }

    private fun muteLoops() {
        for (t in listOf(glideLoop, windLoop, fanLoop)) {
            runCatching { t?.setVolume(0f) }
        }
        lastGlide = 0f; lastWind = 0f; lastFan = 0f
    }

    private fun play(track: AudioTrack?) {
        if (!_enabled || track == null) return
        runCatching {
            track.stop()
            track.playbackHeadPosition = 0
            track.play()
        }
    }

    private fun buildTrack(data: ShortArray): AudioTrack? = runCatching {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(data.size * 2)
            .build()
        track.write(data, 0, data.size)
        track
    }.getOrNull()

    private fun buildLoop(data: ShortArray): AudioTrack? = buildTrack(data)?.apply {
        runCatching { setLoopPoints(0, data.size, -1) }
    }

    // ------------------------------------------------------------- one-shot PCM

    /** Soft low "paper ball hits desk" thud: decaying filtered noise + low sine. */
    private fun thudPcm(): ShortArray {
        val n = (sampleRate * 0.18f).toInt()
        val rnd = Random(42)
        var lp = 0f
        return ShortArray(n) { i ->
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 26f)
            lp += (rnd.nextFloat() * 2f - 1f - lp) * 0.12f // cheap low-pass noise
            val body = sin(2f * PI.toFloat() * 95f * t) * 0.6f
            (((lp * 0.7f + body) * env) * 12000f).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    /** Gentle short tick for score milestones / clean glide bonus. */
    private fun tickPcm(): ShortArray {
        val n = (sampleRate * 0.06f).toInt()
        return ShortArray(n) { i ->
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 70f)
            ((sin(2f * PI.toFloat() * 880f * t) * env) * 9000f).toInt()
                .coerceIn(-32767, 32767).toShort()
        }
    }

    /** Airy swoosh for run start / revive. */
    private fun swooshPcm(): ShortArray {
        val n = (sampleRate * 0.30f).toInt()
        val rnd = Random(7)
        var lp = 0f
        return ShortArray(n) { i ->
            val t = i.toFloat() / sampleRate
            val env = sin(PI.toFloat() * (i.toFloat() / n)) // fade in/out
            lp += (rnd.nextFloat() * 2f - 1f - lp) * (0.05f + 0.25f * (i.toFloat() / n))
            ((lp * env) * 8000f).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    /** Scissors closing: two quick metallic "shing" clicks. */
    private fun snipPcm(): ShortArray {
        val n = (sampleRate * 0.14f).toInt()
        val rnd = Random(11)
        return ShortArray(n) { i ->
            val t = i.toFloat() / sampleRate
            var v = 0f
            // First blade click at t=0, second at t=0.055s.
            for (start in floatArrayOf(0f, 0.055f)) {
                if (t >= start) {
                    val lt = t - start
                    val env = exp(-lt * 110f)
                    val freq = 2600f - lt * 9000f
                    v += (sin(2f * PI.toFloat() * freq * lt) * 0.7f +
                        (rnd.nextFloat() * 2f - 1f) * 0.3f) * env
                }
            }
            (v * 6500f).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    /** Stapler slamming shut: sharp click + low thunk. */
    private fun snapPcm(): ShortArray {
        val n = (sampleRate * 0.16f).toInt()
        val rnd = Random(23)
        var lp = 0f
        return ShortArray(n) { i ->
            val t = i.toFloat() / sampleRate
            val clickEnv = exp(-t * 180f)
            val thunkEnv = exp(-t * 28f)
            lp += (rnd.nextFloat() * 2f - 1f - lp) * 0.5f
            val click = lp * clickEnv
            val thunk = sin(2f * PI.toFloat() * 130f * t) * 0.7f * thunkEnv
            ((click + thunk) * 10500f).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    // ------------------------------------------------------------- loop PCM
    // Loop buffers use integer LFO/oscillator cycle counts so they wrap seamlessly.

    /** Airy glide loop: band-passed noise with a slow 2-cycle swell. */
    private fun glidePcm(): ShortArray {
        val n = (sampleRate * 1.2f).toInt()
        val rnd = Random(101)
        var lp1 = 0f
        var lp2 = 0f
        return ShortArray(n) { i ->
            val white = rnd.nextFloat() * 2f - 1f
            lp1 += (white - lp1) * 0.22f
            lp2 += (lp1 - lp2) * 0.045f
            val air = lp1 - lp2
            val lfo = 0.8f + 0.2f * sin(2f * PI.toFloat() * 2f * i / n)
            (air * lfo * 9000f).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    /** Paper-rustle wind loop: brighter noise with a fast 9-cycle flutter. */
    private fun windPcm(): ShortArray {
        val n = (sampleRate * 1.0f).toInt()
        val rnd = Random(202)
        var lp1 = 0f
        var lp2 = 0f
        return ShortArray(n) { i ->
            val white = rnd.nextFloat() * 2f - 1f
            lp1 += (white - lp1) * 0.45f
            lp2 += (lp1 - lp2) * 0.10f
            val rustle = lp1 - lp2
            val flutter = 0.55f + 0.45f * sin(2f * PI.toFloat() * 9f * i / n)
            (rustle * flutter * 9500f).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    /** Desk-fan loop: low motor hum + harmonic + 4-cycle blade whoosh. */
    private fun fanPcm(): ShortArray {
        val n = sampleRate // exactly 1s so the 96Hz hum loops cleanly
        val rnd = Random(303)
        var lp = 0f
        return ShortArray(n) { i ->
            val t = i.toFloat() / sampleRate
            lp += (rnd.nextFloat() * 2f - 1f - lp) * 0.08f
            val hum = sin(2f * PI.toFloat() * 96f * t) * 0.5f +
                sin(2f * PI.toFloat() * 192f * t) * 0.25f
            val whoosh = 0.75f + 0.25f * sin(2f * PI.toFloat() * 4f * i / n)
            ((hum + lp * 0.35f) * whoosh * 8000f).toInt().coerceIn(-32767, 32767).toShort()
        }
    }
}
