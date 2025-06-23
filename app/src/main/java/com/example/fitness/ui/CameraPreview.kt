package com.example.fitness.ui

// Camera and image libraries
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onImageCaptured: (Bitmap) -> Unit // Callback with the resulting Bitmap
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as LifecycleOwner
    val previewView = remember { PreviewView(context) } // Preview view component
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // Launch camera on composition
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()

        // Create preview use case
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        // Image capture use case
        val imageCaptureConfig = ImageCapture.Builder().build()
        imageCapture = imageCaptureConfig

        try {
            // Unbind previous use cases and bind camera lifecycle
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCaptureConfig
            )
        } catch (exc: Exception) {
            Toast.makeText(context, "Camera bind failed: ${exc.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // UI layout
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Show the live camera feed
        AndroidView(
            factory = { previewView },
            modifier = Modifier.size(280.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Capture button
        Button(onClick = {
            imageCapture?.let { capture ->
                capture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bitmap = imageProxyToBitmap(image) // Convert to Bitmap
                            image.close()
                            bitmap?.let { onImageCaptured(it) } // Trigger callback
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }) {
            Text("Take Photo")
        }
    }
}

/**
 * Converts an ImageProxy (from CameraX) to a Bitmap.
 * Supports both YUV_420_888 (most devices) and JPEG (fallback).
 */
fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    return try {
        when {
            // Most Android devices use YUV format
            image.format == ImageFormat.YUV_420_888 && image.planes.size == 3 -> {
                val yBuffer = image.planes[0].buffer
                val uBuffer = image.planes[1].buffer
                val vBuffer = image.planes[2].buffer

                val ySize = yBuffer.remaining()
                val uSize = uBuffer.remaining()
                val vSize = vBuffer.remaining()

                // Concatenate Y, V, and U into a NV21 byte array
                val nv21 = ByteArray(ySize + uSize + vSize)
                yBuffer.get(nv21, 0, ySize)
                vBuffer.get(nv21, ySize, vSize)
                uBuffer.get(nv21, ySize + vSize, uSize)

                // Compress NV21 to JPEG then decode to Bitmap
                val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
                val out = ByteArrayOutputStream()
                yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
                val imageBytes = out.toByteArray()
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            }

            // Some phones return JPEG directly (simpler conversion)
            image.format == ImageFormat.JPEG && image.planes.size == 1 -> {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }

            else -> null // Unknown format
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
