package com.jamsholat2.android.domain

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.PI

/**
 * Mirrors js/app.js beep() using Web Audio oscillator square wave
 * beep(v,f,t,d) + doubleBeep/longBeep/multipleBeep
 */
class BeepPlayer {

    private var beepJob: Job? = null

    private fun playTone(volume: Float, frequency: Int, durationMs: Int) {
        try {
            val sampleRate = 44100
            val numSamples = (durationMs * sampleRate / 1000.0).toInt()
            val buffer = ShortArray(numSamples)
            val clampedVol = volume.coerceIn(0f, 1f)
            // Generate square wave (matches oscillator.type='square' in js/app.js:169)
            for (i in 0 until numSamples) {
                val angle = 2.0 * PI * frequency * i / sampleRate
                val square = if (sin(angle) >= 0) 1.0 else -1.0
                buffer[i] = (square * Short.MAX_VALUE * clampedVol).toInt().toShort()
            }

            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
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
                .setBufferSizeInBytes(maxOf(minBuf, buffer.size * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            // Release after playback
            Thread {
                try {
                    Thread.sleep(durationMs + 50L)
                } catch (_: InterruptedException) {}
                track.stop()
                track.release()
            }.start()
        } catch (_: Exception) {
            // fallback no-op
        }
    }

    fun beep(volume: Float, frequency: Int, durationMs: Int) {
        playTone(volume, frequency, durationMs)
    }

    fun doubleBeep(volume: Float, frequency: Int) {
        beep(volume, frequency, 120)
        // second after 200ms like js/app.js:181
        Thread {
            try { Thread.sleep(200) } catch (_: Exception) {}
            beep(volume, frequency, 120)
        }.start()
    }

    fun longBeep(volume: Float, frequency: Int) {
        beep(volume, frequency, 1000)
    }

    fun multipleBeep(
        scope: CoroutineScope,
        volume: Float,
        frequency: Int,
        durationMs: Int,
        times: Int
    ) {
        beepJob?.cancel()
        var ctr = 0
        beepJob = scope.launch {
            while (ctr < times) {
                beep(volume, frequency, durationMs)
                ctr++
                if (ctr < times) delay(1000)
            }
        }
    }

    fun cancel() {
        beepJob?.cancel()
    }
}
