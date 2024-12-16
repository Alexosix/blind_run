package com.example.prueba_opencv

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class CreditsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credits)

        // Inicializar el TextView donde se mostrarán los créditos
        val creditsTextView: TextView = findViewById(R.id.creditsTextView)

        // Texto de créditos con el equipo desarrollador
        val creditsText = """
            Equipo Desarrollador:
            
            - Miguel Angel Muñoz: Desarrollador de Backend OpenCV
            - Alexis Yesith Valdes: Desarrollador de Backend OpenCV / Diseño UI/UX
            - Steven Alejandro Mamian: Desarrollador de Android / Diseño interfaz de audio
            - Andres Fernando Pabon: Diseño interfaz de audio
            
            ¡Gracias por usar nuestra aplicación!
        """.trimIndent()

        // Mostrar los créditos en el TextView
        creditsTextView.text = creditsText
    }
}
