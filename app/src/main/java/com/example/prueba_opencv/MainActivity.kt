package com.example.prueba_opencv

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import org.opencv.android.OpenCVLoader


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val buttonToSecond = findViewById<Button>(R.id.buttonToSecond)
        buttonToSecond.setOnClickListener {
            val intent = Intent(this, SecondActivity::class.java)
            startActivity(intent)
        }

        // Inicializa OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Initialization Failed!")
        } else {
            Log.d("OpenCV", "Initialization Successful!")
        }
    }
}