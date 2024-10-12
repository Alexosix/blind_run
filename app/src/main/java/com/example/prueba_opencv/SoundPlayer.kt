package com.example.prueba_opencv

import android.content.Context
import android.media.SoundPool
import android.util.Log

class SoundPlayer(private val context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder().setMaxStreams(1).build()
    private var soundId: Int = 0
    private var currentStreamId: Int = 0  // Guarda el ID del sonido actual

    init {
        soundId = soundPool.load(context, R.raw.audio_clip, 1)
    }

    fun playSound(pitch: Float, pan: Float) {
        val volume = 1.0f  // Volumen máximo

        // Calcular los volúmenes de los canales izquierdo y derecho según el paneo
        val leftVolume = volume * (1.0f - Math.max(0.0f, pan))  // Más pan hacia la derecha disminuye el volumen izquierdo
        val rightVolume = volume * (1.0f - Math.max(0.0f, -pan)) // Más pan hacia la izquierda disminuye el volumen derecho

        // Si ya hay un sonido reproduciéndose, detenerlo antes de reproducir el nuevo
        if (currentStreamId != 0) {
            soundPool.stop(currentStreamId)
        }

        if (soundId != 0) {
            currentStreamId = soundPool.play(soundId, leftVolume, rightVolume, 1, 0, pitch)
            Log.d("Audio", "Reproduciendo sonido con pitch: $pitch, pan: $pan, leftVolume: $leftVolume, rightVolume: $rightVolume")
        } else {
            Log.e("AudioError", "El sonido no se ha cargado correctamente")
        }
    }

    fun release() {
        soundPool.release()
    }
}