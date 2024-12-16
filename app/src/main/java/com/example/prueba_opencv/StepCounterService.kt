package com.example.prueba_opencv

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class StepCounterService : Service(), SensorEventListener {

    private val REQUEST_ACTIVITY_RECOGNITION = 101
    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null
    private var initialStepCount: Int = -1
    private var stepCount: Int = 0
    private var totalDistanceInMeters: Float = 0f // Track total distance

    // Longitud promedio de zancada (en metros)
    private val stepLengthInMeters = 0.762f
    private val distanceThresholdInMeters = 1000f // Set threshold for audio notification
    private var lastAudioNotificationDistance = 0f // Track last distance for audio notification

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaPlayer: MediaPlayer? = null // MediaPlayer instance for audio

    override fun onCreate() {
        super.onCreate()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepCounterSensor == null) {
            Log.e("StepCounterService", "Step Counter Sensor no disponible")
        } else {
            // Verificar y solicitar permisos de reconocimiento de actividad
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("StepCounterService", "Permiso denegado para reconocimiento de actividad")
                stopSelf() // Detener el servicio si no se tienen permisos
            } else {
                startStepSensor()
            }
        }
    }

    // Iniciar el sensor de pasos
    private fun startStepSensor() {
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // Usar una coroutine para procesar el evento en un hilo de fondo
        coroutineScope.launch {
            event?.let {
                if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                    val totalSteps = it.values[0].toInt()

                    if (initialStepCount < 0) {
                        initialStepCount = totalSteps
                    }

                    // Calcular los pasos
                    stepCount = totalSteps - initialStepCount

                    // Calcular la distancia recorrida en metros
                    totalDistanceInMeters = stepCount * stepLengthInMeters

                    // Verificar si se ha alcanzado el umbral de distancia
                    checkDistanceForNotification()
                }
            }
        }
    }

    private fun checkDistanceForNotification() {
        // Verificar si la distancia total supera el último umbral de 10 metros
        if (totalDistanceInMeters - lastAudioNotificationDistance >= distanceThresholdInMeters) {
            playSound()
            lastAudioNotificationDistance = totalDistanceInMeters // Actualizar la última distancia notificada
        }
    }

    private fun playSound() {
        // Release any existing media player
        mediaPlayer?.release()

        // Cambia esto al archivo de audio correcto que añadiste en la carpeta res/raw
        mediaPlayer = MediaPlayer.create(this, R.raw.notification)
        mediaPlayer?.start()

        // Set a listener to release the media player after the sound is played
        mediaPlayer?.setOnCompletionListener {
            it.release()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No se necesita implementación para este caso
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        // Cancelar todas las coroutines para evitar fugas de memoria
        coroutineScope.cancel()
        mediaPlayer?.release() // Release MediaPlayer on destroy
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Este servicio no se vincula
        return null
    }
}
