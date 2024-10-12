package com.example.prueba_opencv

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.TextView
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import kotlinx.coroutines.*

//resolucion
import org.opencv.android.CameraActivity
//import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2

//escala de grises
import org.opencv.core.CvType
import org.opencv.imgproc.Imgproc

//desenfoque gaussiano para disminuir el ruido
import org.opencv.core.Size
//import java.text.DecimalFormat


//region de interes
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Core
import org.opencv.core.MatOfPoint

class SecondActivity : CameraActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private val tag = "OCVSample::Activity"
    private lateinit var mOpenCvCameraView: CameraBridgeViewBase

    private lateinit var distanceLeftTextView: TextView
    private lateinit var distanceRightTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) //mantener la pantalla encendida

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE // siempre horizontal

        setContentView(R.layout.activity_second) // Cambiado a activity_second

        distanceLeftTextView = findViewById(R.id.distanceLeftTextView) //aparecer distancias en pantalla
        distanceRightTextView = findViewById(R.id.distanceRightTextView)

        // Configuración de la vista de la cámara
        mOpenCvCameraView = findViewById(R.id.camera_view)
        mOpenCvCameraView.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView.setMaxFrameSize(640, 480)
        mOpenCvCameraView.setCvCameraViewListener(this)

        // Inicializa OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.d(tag, "OpenCV Initialization Failed!")
        } else {
            Log.d(tag, "OpenCV Initialization Successful!")
        }

        // Botón para volver a MainActivity
        //val buttonBackToMain = findViewById<Button>(R.id.buttonBackToMain)
        //buttonBackToMain.setOnClickListener {
            //processingJob?.cancel() // Cancelar trabajo si está en curso
            //finish() // Termina esta actividad
        //}
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
        var processedFrame: Mat
        runBlocking {
            // Lanza el procesamiento en un contexto de hilo separado (Dispatchers.Default)
            processedFrame = withContext(Dispatchers.Default) {
                processFrame(inputFrame.rgba()) // Llamada a la función de procesamiento
            }
        }
        return processedFrame // Devuelve el frame procesado
    }

    // Método para procesar el frame
    private fun processFrame(mat: Mat): Mat {
        // 1. Convertir a escala de grises
        val gray = Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)

        // 2. Aplicar desenfoque gaussiano para reducir ruido
        val blurred = Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0) //puedo AUMENTAR el desenfoque para eliminar ruido ejem(7,7)

        // 3. Aplicar detección de bordes por Canny
        val edges = Mat(mat.rows(), mat.cols(), CvType.CV_8UC1)
        Imgproc.Canny(blurred, edges, 150.0, 250.0) // AUMENTAR los valores y mantener un rango de 100 disminuye el ruido ejem(200, 300) y disminuye los detalles inecesarios, funciona 100 y 200

        // 4. Aplicar una máscara para la región de interés (ROI), parte inferior de la pantalla
        val mask = Mat.zeros(edges.size(), CvType.CV_8UC1)
        val points = MatOfPoint(
            Point(edges.cols() * 0.1, edges.rows().toDouble()),     // Parte inferior izquierda
            Point(edges.cols() * 0.48, edges.rows() * 0.65),    // Parte superior izquierda (más alta)
            Point(edges.cols() * 0.52, edges.rows() * 0.65),    // Parte superior derecha (más alta)
            Point(edges.cols() * 0.9, edges.rows().toDouble())

        )

        Imgproc.fillPoly(mask, listOf(points), Scalar(255.0)) // mascara para enfocar solo en la region anterior
        val maskedEdges = Mat()
        Core.bitwise_and(edges, mask, maskedEdges)

        // 5. Crear una matriz negra para dibujar las líneas
        //val blackMat = Mat.zeros(mat.size(), mat.type()) // Matriz negra del tamaño de la imagen original

        // 5. Detectar líneas usando la transformada de Hough
        val lines = Mat()
        Imgproc.HoughLinesP(maskedEdges, lines, 1.0, Math.PI / 180, 50, 50.0, 10.0) //
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

        for (i in 0 until lines.rows()) {
            val l = lines.get(i, 0)

            // Coordenadas de la línea detectada
            val x1 = l[0]
            val y1 = l[1]
            val x2 = l[2]
            val y2 = l[3]

            // Cálculo del ángulo para filtrar las líneas horizontales
            val angle = Math.toDegrees(Math.atan2((y2 - y1), (x2 - x1)))

            // Filtrar líneas horizontales: solo dibujar si el ángulo no está cercano a 0 grados
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
            Imgproc.line(
                mat,
                Point(it[0], it[1]), Point(it[2], it[3]),
                Scalar(0.0, 255.0, 0.0), 3
            )
        }

        closestLineRight?.let {
            Imgproc.line(
                mat,
                Point(it[0], it[1]), Point(it[2], it[3]),
                Scalar(255.0, 0.0, 0.0), 3
            )
        }

        provideFeedback(minDistanceToLeft, minDistanceToRight) // Proporcionar retroalimentación

        gray.release()
        blurred.release()
        edges.release()
        mask.release()
        maskedEdges.release()
        lines.release()

    return mat // Devolver la imagen con las líneas de carriles dibujadas
}

    private fun provideFeedback(distanceLeft: Double, distanceRight: Double) {

        // Redondear a valores enteros
        val roundedDistanceLeft = distanceLeft.toInt()
        val roundedDistanceRight = distanceRight.toInt()

        // Actualiza los TextView con las distancias redondeadas
        runOnUiThread {
            distanceLeftTextView.text = "$roundedDistanceLeft"
            distanceRightTextView.text = "$roundedDistanceRight"
        }

        // Retroalimentación adicional como sonidos o vibraciones
        Log.d(tag, "L: ${distanceLeft.toString().take(2)} px, R: ${distanceRight.toString().take(2)} px")
    }

}