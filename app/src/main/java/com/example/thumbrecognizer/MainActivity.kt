@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.thumbrecognizer

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.thumbrecognizer.ui.theme.ThumbRecognizerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(this, CAMERAX_PERMISSIONS, 0)
        }

        setContent {
            ThumbRecognizerTheme {
                val viewModel = viewModel<MainViewModel>()
                val bitmap by viewModel.bitmap.collectAsState()
                val croppedBitmap by viewModel.croppedBitmap.collectAsState()
                var showCrop by remember { mutableStateOf(false) }
                val controller = remember {
                    LifecycleCameraController(applicationContext).apply {
                        setEnabledUseCases(CameraController.IMAGE_CAPTURE)
                    }
                }

                when {
                    croppedBitmap != null -> FinalPhotoScreen(croppedBitmap!!)
                    bitmap != null -> PhotoReviewScreen(
                        bitmap = bitmap!!,
                        onRetake = { viewModel.clearPhoto() },
                        onProceed = { showCrop = true }
                    )
                    else -> CameraScreen(
                        controller = controller,
                        onPhotoTaken = viewModel::onPhotoTaken
                    )
                }

                if (showCrop && bitmap != null) {
                    CircularCropper(
                        bitmap = bitmap!!,
                        onCropConfirmed = {
                            viewModel.onCropConfirmed(it)
                            showCrop = false
                        }
                    )
                }
            }
        }
    }

    private fun takePhoto(controller: LifecycleCameraController, onPhotoTaken: (Bitmap) -> Unit) {
        controller.takePicture(
            ContextCompat.getMainExecutor(applicationContext),
            object : OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                    }
                    val rotatedBitmap = Bitmap.createBitmap(
                        image.toBitmap(), 0, 0,
                        image.width, image.height,
                        matrix, true
                    )
                    onPhotoTaken(rotatedBitmap)
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("Camera", "Capture error", exception)
                }
            }
        )
    }

    private fun hasRequiredPermissions(): Boolean {
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(applicationContext, it) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val CAMERAX_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}