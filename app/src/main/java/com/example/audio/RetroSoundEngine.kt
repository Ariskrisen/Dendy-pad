package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.min

class RetroSoundEngine {

    private val executor = Executors.newSingleThreadExecutor()
    private val sampleRate = 22050

    private val dpadClickSamples: ShortArray = buildSquareClick(freqHz = 420.0, durationMs = 12, volume = 0.12f)
    private val abClickSamples: ShortArray = buildSquareClick(freqHz = 680.0, durationMs = 15, volume = 0.15f)
    private val startClickSamples: ShortArray = buildSquareClick(freqHz = 520.0, durationMs = 20, volume = 0.14f)

    fun playDpadClick() = playSamples(dpadClickSamples)

    fun playActionClick() = playSamples(abClickSamples)

    fun playSystemClick() = playSamples(startClickSamples)

    private fun playSamples(samples: ShortArray) {
        executor.execute {
            try {
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
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                track.play()
                Thread.sleep(28L)
                track.stop()
                track.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun buildSquareClick(freqHz: Double, durationMs: Int, volume: Float): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val data = ShortArray(count)
        val period = sampleRate / freqHz
        for (i in 0 until count) {
            val env = 1.0 - (i.toDouble() / count.toDouble())
            val wave = if ((i % period) < (period / 2.0)) 1.0 else -1.0
            val sample = (wave * env * volume * Short.MAX_VALUE).toInt()
            data[i] = min(Short.MAX_VALUE.toInt(), sample).toShort()
        }
        return data
    }

    fun release() {
        executor.shutdownNow()
    }
}
