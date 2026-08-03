package com.paperfly.paperplanedrift.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Tiny procedural sound engine — no audio assets needed.
 * Synthesizes a soft paper "thud" for crashes and a gentle tick for milestones.
 */
class SoundManager {

    @Volatile
    var enabled: Boolean = true

    private val sampleRate = 22050

    private val thudTrack: AudioTrack? by lazy { buildTrack(thudPcm()) }
    private val tickTrack: AudioTrack? by lazy { buildTrack(tickPcm()) }
    private val swooshTrack: AudioTrack? by lazy { buildTrack(swooshPcm()) }

    fun playThud() = play(thudTrack)
    fun playTick() = play(tickTrack)
    fun playSwoosh() = play(swooshTrack)

    private fun play(track: AudioTrack?) {
        if (!enabled || track == null) return
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
}
