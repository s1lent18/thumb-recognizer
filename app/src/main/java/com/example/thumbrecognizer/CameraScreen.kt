package com.example.thumbrecognizer

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraScreen(
    controller: LifecycleCameraController,
    onPhotoTaken: (Bitmap) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    AndroidView(
        factory = {
            PreviewView(it).apply {
                this.controller = controller
                controller.bindToLifecycle(lifecycleOwner)
            }
        },
        modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction = 0.8f)
    )

    Box(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction = 0.95f),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = {
                    controller.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val matrix = Matrix().apply {
                                    postRotate(image.imageInfo.rotationDegrees.toFloat())
                                }
                                val rotatedBitmap = Bitmap.createBitmap(
                                    image.toBitmap(),
                                    0, 0,
                                    image.width, image.height,
                                    matrix, true
                                )
                                onPhotoTaken(rotatedBitmap)
                                image.close()
                            }
                        }
                    )
                },
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Take Photo", modifier = Modifier.size(25.dp))
            }
        }
    }
}