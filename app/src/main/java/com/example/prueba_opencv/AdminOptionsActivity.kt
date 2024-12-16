package com.example.prueba_opencv

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class AdminOptionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_options)
        val aboutButton = findViewById<Button>(R.id.option2)
        // Configurar el OnClickListener para el botón "About"
        aboutButton.setOnClickListener{
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }
        val credditsButton = findViewById<Button>(R.id.option3)
        // Configurar el OnClickListener para el botón "About"
        credditsButton.setOnClickListener{
            val intent = Intent(this, CreditsActivity::class.java)
            startActivity(intent)
        }
    }
}