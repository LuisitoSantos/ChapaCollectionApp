/*package com.tuempresa.chapacollectionapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

class CustomCameraActivity : ComponentActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraScreen()
        }
    }

    @Composable
    fun CameraScreen() {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        AndroidView(
            factory = { ctx ->
                previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                startCamera(context, lifecycleOwner)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Botón para capturar la imagen
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize()
        ) {
            Button(
                onClick = { takePhoto() },
                modifier = Modifier
                    .padding(16.dp)
                    .size(60.dp)
            ) {
                Text("📷")
            }
        }

        // Guía visual: Círculo o Cuadrícula
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Dibujar un círculo en el centro
                    val radius = size.minDimension / 3
                    drawCircle(
                        color = Color.White,
                        radius = radius,
                        center = Offset(size.width / 2, size.height / 2),
                        style = Stroke(width = 4.dp.toPx())
                    )

                    // Opcional: Dibujar una cuadrícula
                    val thirdWidth = size.width / 3
                    val thirdHeight = size.height / 3

                    for (i in 1..2) {
                        // Líneas verticales
                        drawLine(
                            color = Color.White,
                            start = Offset(x = thirdWidth * i, y = 0f),
                            end = Offset(x = thirdWidth * i, y = size.height),
                            strokeWidth = 2.dp.toPx()
                        )
                        // Líneas horizontales
                        drawLine(
                            color = Color.White,
                            start = Offset(x = 0f, y = thirdHeight * i),
                            end = Offset(x = size.width, y = thirdHeight * i),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
        )
    }

    private fun startCamera(context: Context, lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Log.e("CameraX", "Error al iniciar la cámara", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun takePhoto() {
        val photoFile = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "chapa_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraX", "Error al guardar la imagen", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Log.d("CameraX", "Imagen guardada en: $savedUri")
                    // Puedes devolver el URI a la actividad anterior si lo deseas
                    val intent = Intent().apply {
                        putExtra("image_uri", savedUri.toString())
                    }
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            }
        )
    }
}

 */