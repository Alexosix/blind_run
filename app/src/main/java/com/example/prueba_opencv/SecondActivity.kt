package com.example.prueba_opencv


import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import kotlinx.coroutines.*

//resolucion
import org.opencv.android.CameraActivity

//escala de grises
import org.opencv.core.CvType
import org.opencv.imgproc.Imgproc

//desenfoque gaussiano para disminuir el ruido
import org.opencv.core.Size


//region de interes
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Core
import org.opencv.core.MatOfPoint

class SecondActivity : CameraActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private val TAG = "OCVSample::Activity"
    private lateinit var mOpenCvCameraView: CameraBridgeViewBase

    //private lateinit var distanceLeftTextView: TextView
    //private lateinit var distanceRightTextView: TextView

    private val leftDistances = mutableListOf<Double>() // variables para suavizar las distancias
    private val rightDistances = mutableListOf<Double>()
    private val maxHistorySize = 5  // Tamaño del historial para suavizar

    private var centerCalculated = false // Flag to track if center has been calculated
    private lateinit var mediaPlayer: MediaPlayer

    //Sound module inicialitation
    private lateinit var soundPlayer: SoundPlayer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soundPlayer = SoundPlayer(this)
        mediaPlayer = MediaPlayer.create(this, R.raw.detectado)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) //mantener la pantalla encendida

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE // siempre horizontal

        setContentView(R.layout.activity_second) // Cambiado a activity_second

        //distanceLeftTextView = findViewById(R.id.distanceLeftTextView) //aparecer distancias en pantalla
        //distanceRightTextView = findViewById(R.id.distanceRightTextView)

        // Configuración de la vista de la cámara
        mOpenCvCameraView = findViewById(R.id.camera_view)
        mOpenCvCameraView?.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView.setMaxFrameSize(640, 480)
        mOpenCvCameraView.setCvCameraViewListener(this)

        // Inicializa OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV Initialization Failed!")
        } else {
            Log.d(TAG, "OpenCV Initialization Successful!")
        }

        // Botón para volver a MainActivity
        val buttonBackToMain = findViewById<Button>(R.id.buttonBackToMain)
        buttonBackToMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            soundPlayer.release() // Liberar el MediaPlayer al volver a la actividad principal
            soundPlayer.stopSound() // Detener el sonido si se estaba reproduciendo
        //processingJob?.cancel() // Cancelar trabajo si está en curso
        finish() // Termina esta actividad
        }
    }

    override fun onPause() {
        super.onPause()
        mOpenCvCameraView.disableView()
    }

    override fun onResume() {
        super.onResume()
        mOpenCvCameraView.enableView()
    }

    override fun getCameraViewList(): MutableList<CameraBridgeViewBase> {
        return mutableListOf(mOpenCvCameraView)
    }

    override fun onDestroy() {
        super.onDestroy()
        mOpenCvCameraView.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        //mOpenCvCameraView.setRotation(45F) // Ajusta el ángulo según sea necesario
    }

    override fun onCameraViewStopped() {}

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        // Coroutines scope para el procesamiento en segundo plano
        var processedFrame: Mat = inputFrame.rgba()
        runBlocking {
            // Lanza el procesamiento en un contexto de hilo separado (Dispatchers.Default)
            processedFrame = withContext(Dispatchers.Default) {
                processFrame(inputFrame.rgba()) // Llamada a la función de procesamiento
            }
        }
        return processedFrame // Devuelve el frame procesado
    }

    private fun resetDistanceHistory() {
        leftDistances.clear()
        rightDistances.clear()
    }
    // Método para procesar el frame
    private fun processFrame(mat: Mat): Mat {

        resetDistanceHistory()

        // 1. Convertir a escala de grises
        val gray = Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)

        // 2. Aplicar desenfoque gaussiano para reducir ruido
        val blurred = Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0) //puedo AUMENTAR el desenfoque para eliminar ruido ejem(7,7)

        // 3. Aplicar detección de bordes por Canny
        val edges = Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
        Imgproc.Canny(blurred, edges, 100.0, 200.0) // AUMENTAR los valores y mantener un rango de 100 disminuye el ruido ejem(200, 300) y disminuye los detalles inecesarios, funciona 100 y 200

        // 4. Aplicar una máscara para la región de interés (ROI), parte inferior de la pantalla
        val mask = Mat.zeros(edges.size(), CvType.CV_8UC1)
        val points = MatOfPoint(
            Point(edges.cols() * 0.0, edges.rows().toDouble()),          // Parte inferior izquierda
            Point(edges.cols() * 0.48, edges.rows() * 0.65),              // Parte superior izquierda (más alta)
            Point(edges.cols() * 0.52, edges.rows() * 0.65),              // Parte superior derecha (más alta)
            Point(edges.cols() * 1.0, edges.rows().toDouble())

        )

        Imgproc.fillPoly(mask, listOf(points), Scalar(255.0)) // mascara para enfocar solo en la region anterior
        val maskedEdges = Mat()
        Core.bitwise_and(edges, mask, maskedEdges)

        // 5. Crear una matriz negra para dibujar las líneas
        //val blackMat = Mat.zeros(mat.size(), mat.type()) // Matriz negra del tamaño de la imagen original

        // 5. Detectar líneas usando la transformada de Hough
        val lines = Mat()
        Imgproc.HoughLinesP(maskedEdges, lines, 0.5, Math.PI / 180, 50, 30.0, 10.0) //
        //AUMENTAR rho reducir la precisión en la detección de líneas, pero mejorar el rendimiento. (entre 1-2)
        //MANTENER valor angular theta, Un valor menor ofrece una mayor precisión angular.
        //AUMENTAR threshold detectará solo las líneas más prominentes, mientras que un valor más bajo detectará más líneas, incluidas las más débiles. (en prueba subir valor)
        //AUMENTAR minline.Ajusta este valor según el tamaño de las líneas que deseas detectar. Si los carriles son claros y largos, puedes aumentar este valor.
        //AUMENTAR maxline si se tienen carriles interrumpidos (curvas en el CDU)

        // 6. Detectar la línea más cercana al centro de la imagen
        val centerX = edges.cols() / 2
        var closestLineLeft: DoubleArray? = null
        var closestLineRight: DoubleArray? = null
        var minDistanceToLeft = Double.MAX_VALUE
        var minDistanceToRight = Double.MAX_VALUE

        val minDistanceThreshold = 30.0

        for (i in 0 until lines.rows()) {
            val l = lines.get(i, 0)

            // Coordenadas de la línea detectada
            val x1 = l[0]
            val y1 = l[1]
            val x2 = l[2]
            val y2 = l[3]

            // Cálculo del ángulo para filtrar las líneas horizontales
            val angle = Math.toDegrees(Math.atan2((y2 - y1), (x2 - x1)))
            if (angle < -10 || angle > 10) {
                // Cálculo del centro de la línea
                val startX = l[0]
                val endX = l[2]
                val lineCenterX = (startX + endX) / 2

                // Calcular distancias de la línea más cercana a la izquierda y derecha
                if (lineCenterX < centerX) {
                    // Línea a la izquierda
                    val distanceToCenter = centerX - lineCenterX
                    if (distanceToCenter < minDistanceToLeft) {
                        minDistanceToLeft = distanceToCenter
                        closestLineLeft = l
                    }
                } else {
                    // Línea a la derecha
                    val distanceToCenter = lineCenterX - centerX
                    if (distanceToCenter < minDistanceToRight) {
                        minDistanceToRight = distanceToCenter
                        closestLineRight = l
                    }
                }
            }
        }

        // 7. Dibujar las líneas más cercanas
        closestLineLeft?.let {
            if (it[0] < centerX && it[2] < centerX) { // Solo dibujar si ambos puntos están a la izquierda
                Imgproc.line(
                    mat,
                    Point(it[0], it[1]), Point(it[2], it[3]),
                    Scalar(0.0, 255.0, 0.0), 3
                )
            }
        }

        closestLineRight?.let {
            if (it[0] > centerX && it[2] > centerX) { // Solo dibujar si ambos puntos están a la derecha
                Imgproc.line(
                    mat,
                    Point(it[0], it[1]), Point(it[2], it[3]),
                    Scalar(255.0, 0.0, 0.0), 3
                )
            }
        }

        // Agregar las nuevas distancias a las listas de historial
        leftDistances.add(minDistanceToLeft)
        rightDistances.add(minDistanceToRight)

        // Limitar el tamaño del historial (mantener solo los últimos maxHistorySize elementos)
        if (leftDistances.size > maxHistorySize) {
            leftDistances.removeAt(0)
        }
        if (rightDistances.size > maxHistorySize) {
            rightDistances.removeAt(0)
        }

        // Calcular el promedio de las últimas distancias
        val smoothedDistanceLeft = leftDistances.average()
        val smoothedDistanceRight = rightDistances.average()

        // Redondear a valores enteros
        val roundedDistanceLeft = smoothedDistanceLeft.toInt()
        val roundedDistanceRight = smoothedDistanceRight.toInt()

        provideFeedback(roundedDistanceLeft, roundedDistanceRight) // Proporcionar retroalimentación

        gray.release()
        blurred.release()
        edges.release()
        mask.release()
        maskedEdges.release()
        lines.release()

        return mat // Devolver la imagen con las líneas de carriles dibujadas
    }

    private fun provideFeedback(distanceLeft: Int, distanceRight: Int) {

        handleSoundPlayback(distanceLeft.toFloat(), distanceRight.toFloat())

        /*// Actualiza los TextView con las distancias suavizadas y redondeadas
        runOnUiThread {
            distanceLeftTextView.text = "$distanceLeft"
            distanceRightTextView.text = "$distanceRight"
        }*/

        if (distanceLeft > 50 && distanceRight > 50 &&
            distanceLeft < 150 && distanceRight < 150 && // Adjust the range as needed
            centerCalculated == false // Check if center has been calculated before
        ) {
            // Calculate and log the center point
            //val centerX = (distanceLeft + distanceRight) / 2
            //Log.d(TAG, "Center X: $centerX")

            // Play the sound to indicate detection
            playSound() // You'll need to implement this function

            centerCalculated = true // Set the flag to prevent further executions
        }
        // Retroalimentación adicional como sonidos o vibraciones
        Log.d(TAG, "L: ${distanceLeft.toString().take(2)} px, R: ${distanceRight.toString().take(2)} px")
    }

    fun handleSoundPlayback(distanceLeft: Float, distanceRight: Float) {
        // Define el rango de operación
        val minRange = 0f
        val maxRange = 70f

        // Verifica si ambas distancias están dentro del rango
        if (distanceLeft in minRange..maxRange && distanceRight in minRange..maxRange) {
            // Si ambas distancias están en el rango, no reproducir sonido
            return
        }

        // Si no, ejecuta la función para reproducir sonido
        soundPlayer.playSoundBasedOnDistance(distanceLeft, distanceRight)
    }


    private fun playSound() {
        if (this::mediaPlayer.isInitialized) { // Check if initialized (optional)
            mediaPlayer.start()
        } else {
            Log.e(TAG, "MediaPlayer not initialized!")
        }
    }
}