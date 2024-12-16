package com.example.prueba_opencv

import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // Opcional: Configuración del VideoView
       // val videoView = findViewById<VideoView>(R.id.videoView)
       /* val videoPath = "android.resource://" + packageName + "/" + R.raw.tutorial_video // Asegúrate de tener un archivo de video en res/raw
        videoView.setVideoURI(Uri.parse(videoPath))
        videoView.start() // Reproduce el video automáticamente (opcional)*/
    }
}