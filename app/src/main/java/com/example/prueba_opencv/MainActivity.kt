package com.example.prueba_opencv

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import org.opencv.android.OpenCVLoader

class MainActivity : AppCompatActivity() {
    // Variables para el control del botón de bajar volumen
    private var volumeDownPressCount = 0
    private var isVolumeDownPressed = false
    private val maxPressTime = 25L //
    private var handler: Handler = Handler(Looper.getMainLooper()) // Handler para manejar el retraso

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val buttonToSecond = findViewById<Button>(R.id.buttonToSecond)
        buttonToSecond.setOnClickListener {
            val intent = Intent(this, SecondActivity::class.java)
            startActivity(intent)

            // Iniciar el servicio del StepCounter
            val stepCounterIntent = Intent(this, StepCounterService::class.java)
            startService(stepCounterIntent) // Cambia a startService para iniciar el servicio en segundo plano
        }

        val adminOptionsTextView = findViewById<TextView>(R.id.adminOptions)
        adminOptionsTextView.setOnClickListener {
            val intent = Intent(this, AdminOptionsActivity::class.java)
            startActivity(intent)
        }

        // Inicializa OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Initialization Failed!")
        } else {
            Log.d("OpenCV", "Initialization Successful!")
        }
    }
//Funcion para iniciar la app con el boton de volumen
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && !isVolumeDownPressed) {
            isVolumeDownPressed = true
            volumeDownPressCount++

            handler.postDelayed({
                volumeDownPressCount = 0 // Reiniciar el contador si pasa más de maxPressTime entre pulsaciones
            }, maxPressTime)

            if (volumeDownPressCount == 2) { // Si se presiona 3 veces
                val intent = Intent(this, SecondActivity::class.java)
                startActivity(intent)
                volumeDownPressCount = 0 // Reiniciar el contador después de la acción
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    // Capturar el evento del botón de subir volumen
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            isVolumeDownPressed = false
            handler.removeCallbacksAndMessages(null) // Cancelar el handler si el botón se suelta antes de 1.5 segundos
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}