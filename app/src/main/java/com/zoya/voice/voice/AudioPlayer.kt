package com.zoya.voice.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioManager
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Plays raw PCM 16-bit little-endian mono audio @ 24 kHz
 * (Gemini Live native audio output rate).
 */
class AudioPlayer {

    companion object {
        const val SAMPLE_RATE = 24000
        private val BUFFER_SIZE = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2
    }

    private var audioTrack: AudioTrack? = null
    private val queue = LinkedBlockingQueue<ByteArray>()
    private val isPlaying = AtomicBoolean(false)
    private var playerThread: Thread? = null

    fun start() {
        if (isPlaying.get()) return

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(BUFFER_SIZE)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        isPlaying.set(true)

        playerThread = thread(name = "ZoyaAudioPlayer") {
            while (isPlaying.get()) {
                try {
                    val chunk = queue.take()
                    audioTrack?.write(chunk, 0, chunk.size)
                } catch (_: InterruptedException) {
                    break
                } catch (_: Exception) {}
            }
        }
    }

    fun play(pcmData: ByteArray) {
        if (!isPlaying.get()) start()
        queue.offer(pcmData)
    }

    fun stop() {
        isPlaying.set(false)
        playerThread?.interrupt()
        playerThread = null
        queue.clear()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    fun interrupt() {
        // Clear queued audio so user interruption is immediate
        queue.clear()
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        } catch (_: Exception) {}
    }
}