package com.example.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object AudioSynthHelper {

    private const val SAMPLE_RATE = 22050 // 22.05 kHz stereo for fast generation & crisp sound

    suspend fun getOrCreateTrackFile(context: Context, trackKey: String): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "$trackKey.wav")
        if (file.exists() && file.length() > 1000) {
            return@withContext file
        }

        val durationSeconds = 32 // 32 seconds rich musical loop
        val totalSamples = SAMPLE_RATE * durationSeconds
        val numChannels = 2
        val bytesPerSample = 2 // 16-bit PCM
        val dataSize = totalSamples * numChannels * bytesPerSample

        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        writeWavHeader(buffer, dataSize, SAMPLE_RATE, numChannels)

        when (trackKey) {
            "neon_horizon" -> generateSynthwave(buffer, totalSamples, SAMPLE_RATE)
            "emerald_aurora" -> generateAmbient(buffer, totalSamples, SAMPLE_RATE)
            "morning_espresso" -> generateAcoustic(buffer, totalSamples, SAMPLE_RATE)
            else -> generateLofiChill(buffer, totalSamples, SAMPLE_RATE)
        }

        FileOutputStream(file).use { fos ->
            fos.write(buffer.array())
        }

        file
    }

    private fun writeWavHeader(buffer: ByteBuffer, dataSize: Int, sampleRate: Int, channels: Int) {
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16) // Subchunk1Size for PCM
        buffer.putShort(1) // AudioFormat 1 = PCM
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * channels * 2) // ByteRate
        buffer.putShort((channels * 2).toShort()) // BlockAlign
        buffer.putShort(16) // BitsPerSample
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataSize)
    }

    // 1. Lofi Chillhop: Warm chords (Dm9 -> G13 -> Cmaj9 -> Am7), vinyl crackle, gentle beat
    private fun generateLofiChill(buffer: ByteBuffer, totalSamples: Int, sampleRate: Int) {
        val chordFreqs = listOf(
            listOf(146.83, 220.0, 261.63, 329.63), // Dm9
            listOf(196.00, 246.94, 329.63, 392.00), // G13
            listOf(130.81, 196.00, 246.94, 293.66), // Cmaj9
            listOf(110.00, 164.81, 220.00, 261.63)  // Am7
        )
        val chordDuration = sampleRate * 4 // 4 seconds per chord
        val beatSamples = sampleRate / 2   // ~120 bpm eighths

        for (i in 0 until totalSamples) {
            val chordIndex = ((i / chordDuration) % chordFreqs.size)
            val currentChord = chordFreqs[chordIndex]
            val t = i.toDouble() / sampleRate

            // Chords with soft warm harmonics
            var chordSignal = 0.0
            for ((idx, freq) in currentChord.withIndex()) {
                val phase = 2.0 * PI * freq * t
                val vibrato = 1.0 + 0.003 * sin(2.0 * PI * 4.5 * t)
                val fundamental = sin(phase * vibrato)
                val secondHarmonic = 0.35 * sin(2.0 * phase * vibrato)
                chordSignal += (fundamental + secondHarmonic) * (0.8 / (idx + 1))
            }
            chordSignal /= currentChord.size

            // Smooth bass
            val rootFreq = currentChord[0] / 2.0
            val bass = 0.45 * sin(2.0 * PI * rootFreq * t)

            // Gentle kick/snare pulse
            val beatPhase = (i % beatSamples).toDouble() / beatSamples
            val drumEnvelope = kotlin.math.exp(-12.0 * beatPhase)
            val beatNum = (i / beatSamples) % 4
            val drum = if (beatNum == 0 || beatNum == 2) {
                // Soft low kick
                0.35 * sin(2.0 * PI * (65.0 * (1.0 - beatPhase * 0.7)) * t) * drumEnvelope
            } else {
                // Soft snare brush / crackle
                val noise = ((i * 1103515245 + 12345) and 0x7FFFFFFF) / 2147483648.0 - 0.5
                0.22 * noise * drumEnvelope
            }

            // Subtle vinyl noise
            val vinyl = (((i * 1664525 + 1013904223) and 0x7FFFFFFF) / 2147483648.0 - 0.5) * 0.02

            val mixedL = (chordSignal * 0.38 + bass * 0.35 + drum * 0.32 + vinyl).coerceIn(-0.95, 0.95)
            val mixedR = (chordSignal * 0.36 + bass * 0.35 + drum * 0.32 - vinyl).coerceIn(-0.95, 0.95)

            buffer.putShort((mixedL * 32767.0).toInt().toShort())
            buffer.putShort((mixedR * 32767.0).toInt().toShort())
        }
    }

    // 2. Synthwave / Retrowave: Upbeat 80s bassline, analog chords, arpeggios
    private fun generateSynthwave(buffer: ByteBuffer, totalSamples: Int, sampleRate: Int) {
        val bassNotes = listOf(110.0, 110.0, 87.31, 87.31, 65.41, 65.41, 98.0, 98.0) // A -> F -> C -> G
        val stepSamples = (sampleRate * 0.25).toInt() // 120 bpm 16th/8th steps

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val stepIdx = (i / stepSamples) % bassNotes.size
            val baseFreq = bassNotes[stepIdx]

            val notePhase = (i % stepSamples).toDouble() / stepSamples
            val decay = kotlin.math.exp(-4.0 * notePhase)

            // Synth bass (saw wave approximation)
            var saw = 0.0
            for (h in 1..4) {
                saw += sin(2.0 * PI * baseFreq * h * t) / h
            }
            val bass = saw * 0.35 * decay

            // Plucky retro lead arpeggio
            val arpMultiplier = listOf(1.0, 1.25, 1.5, 1.875)[(i / (stepSamples / 2)) % 4]
            val leadFreq = baseFreq * 2.0 * arpMultiplier
            val lead = 0.25 * sin(2.0 * PI * leadFreq * t) * kotlin.math.exp(-6.0 * ((i % (stepSamples / 2)).toDouble() / (stepSamples / 2)))

            // Kick pulse on beats 0, 2
            val beatPhase = (i % (stepSamples * 2)).toDouble() / (stepSamples * 2)
            val kick = 0.35 * sin(2.0 * PI * (90.0 * (1.0 - beatPhase * 0.8)) * t) * kotlin.math.exp(-15.0 * beatPhase)

            val left = (bass + lead * 0.8 + kick).coerceIn(-0.95, 0.95)
            val right = (bass + lead * 0.6 + kick).coerceIn(-0.95, 0.95)

            buffer.putShort((left * 32767.0).toInt().toShort())
            buffer.putShort((right * 32767.0).toInt().toShort())
        }
    }

    // 3. Ambient / Aurora: Dreamy evolving pads, tranquil resonances
    private fun generateAmbient(buffer: ByteBuffer, totalSamples: Int, sampleRate: Int) {
        val droneFreqs = listOf(164.81, 246.94, 329.63, 493.88) // E minor / ethereal

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            var pad = 0.0
            for ((idx, freq) in droneFreqs.withIndex()) {
                val lfo = sin(2.0 * PI * (0.15 + idx * 0.05) * t) * 0.5 + 0.5
                val tone = sin(2.0 * PI * freq * t) + 0.3 * sin(2.0 * PI * (freq * 1.5) * t)
                pad += tone * lfo * 0.22
            }

            // Celestial sparkle
            val shimmer = 0.12 * sin(2.0 * PI * (659.25 + 5.0 * sin(2.0 * PI * 0.4 * t)) * t)

            val left = (pad + shimmer * 0.7).coerceIn(-0.95, 0.95)
            val right = (pad + shimmer * 0.4).coerceIn(-0.95, 0.95)

            buffer.putShort((left * 32767.0).toInt().toShort())
            buffer.putShort((right * 32767.0).toInt().toShort())
        }
    }

    // 4. Acoustic Morning: Warm acoustic pluck progression
    private fun generateAcoustic(buffer: ByteBuffer, totalSamples: Int, sampleRate: Int) {
        val notes = listOf(261.63, 329.63, 392.00, 523.25, 293.66, 369.99, 440.00, 587.33) // C & D chords
        val step = (sampleRate * 0.35).toInt()

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val noteIdx = (i / step) % notes.size
            val f = notes[noteIdx]
            val noteTime = (i % step).toDouble() / step
            val pluck = (sin(2.0 * PI * f * t) + 0.4 * sin(2.0 * PI * 2 * f * t)) * kotlin.math.exp(-5.0 * noteTime)

            val left = (pluck * 0.45).coerceIn(-0.95, 0.95)
            val right = (pluck * 0.42).coerceIn(-0.95, 0.95)

            buffer.putShort((left * 32767.0).toInt().toShort())
            buffer.putShort((right * 32767.0).toInt().toShort())
        }
    }
}
