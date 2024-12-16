package com.example.prueba_opencv

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.media.PlaybackParams

import android.os.Handler
import android.os.Looper

class SoundPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying: Boolean = false
    private var lastMinL: Float = -1f
    private var lastMinR: Float = -1f
    private val handler = Handler(Looper.getMainLooper()) // Handler para manejar intervalos
    private var interval: Long = 2000 // Intervalo en milisegundos (2 segundos)
    private var shouldPlayIntermittently: Boolean = false

    var minDistance: Float = 0f
    var maxDistance: Float = 70f

    init {
        mediaPlayer = MediaPlayer.create(context, R.raw.a4)
        mediaPlayer?.setOnCompletionListener {
            isPlaying = false
        }
    }

    private fun mapToPitch(value: Float, min: Float, max: Float): Float {
        val pitchMin = 0.5f
        val pitchMax = 2.0f
        return pitchMax - (pitchMax - pitchMin) * ((value - min) / (max - min))
    }

    fun playSoundBasedOnDistance(minL: Float, minR: Float) {
        if (minL == lastMinL && minR == lastMinR) {
            return
        }

        lastMinL = minL
        lastMinR = minR

        if (minL in minDistance..maxDistance || minR in minDistance..maxDistance) {
            val pitchL = if (minL in minDistance..maxDistance) mapToPitch(minL, minDistance, maxDistance) else 1.0f
            val pitchR = if (minR in minDistance..maxDistance) mapToPitch(minR, minDistance, maxDistance) else 1.0f

            val pan = when {
                minL < minR -> -1.0f
                minR < minL -> 1.0f
                else -> 0.0f
            }

            if (shouldPlayIntermittently) {
                startIntermittentSound(pitchL.coerceAtLeast(pitchR), pan)
            } else {
                playSound(pitchL.coerceAtLeast(pitchR), pan)
            }
        } else if (isPlaying) {
            stopSound()
        }
    }

    fun playSound(pitch: Float, pan: Float) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, R.raw.a4)
        }

        val leftVolume = 1.0f * (1.0f - Math.max(0.0f, pan))
        val rightVolume = 1.0f * (1.0f - Math.max(0.0f, -pan))
        mediaPlayer?.setVolume(leftVolume, rightVolume)

        val params = PlaybackParams()
        params.pitch = pitch
        mediaPlayer?.playbackParams = params

        mediaPlayer?.start()
        isPlaying = true

        Log.d("Audio", "Reproduciendo sonido con pitch: $pitch, pan: $pan")
    }

    fun stopSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        Log.d("Audio", "Sonido detenido")
    }

    fun startIntermittentSound(pitch: Float, pan: Float) {
        shouldPlayIntermittently = true
        handler.post(object : Runnable {
            override fun run() {
                if (shouldPlayIntermittently) {
                    playSound(pitch, pan)
                    handler.postDelayed(this, interval) // Volver a ejecutar en "interval" milisegundos
                }
            }
        })
    }

    fun stopIntermittentSound() {
        shouldPlayIntermittently = false
        handler.removeCallbacksAndMessages(null) // Detener el handler
        stopSound()
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
