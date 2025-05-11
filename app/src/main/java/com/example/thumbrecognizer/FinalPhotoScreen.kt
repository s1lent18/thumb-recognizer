package com.example.thumbrecognizer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

@Composable
fun FinalPhotoScreen(
    bitmap: Bitmap,
    uploadUrl: String = "http://192.168.100.9:8080/match"
) {
    var uploading by remember { mutableStateOf(false) }
    var uploadResult by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(300.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    uploading = true
                    uploadCroppedImageForMatching(bitmap, uploadUrl) { success, response ->
                        uploading = false
                        uploadResult = if (success) {
                            "Upload successful: $response"
                        } else {
                            "Upload failed: $response"
                        }
                    }
                },
                enabled = !uploading
            ) {
                Text(if (uploading) "Uploading..." else "Upload")
            }

            if (uploadResult != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(uploadResult ?: "")
            }
        }
    }
}
