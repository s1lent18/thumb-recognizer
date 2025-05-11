package com.example.thumbrecognizer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

@Composable
fun FinalPhotoScreen(
    bitmap: Bitmap,
    baseUrl: String = "http://192.168.100.9:8080"
) {
    var name by remember { mutableStateOf("") }
    var uploading by remember { mutableStateOf(false) }
    var matching by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

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

            Spacer(modifier = Modifier.height(16.dp))

            // Name input for upload
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name for Upload") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Match button
            Button(
                onClick = {
                    matching = true
                    resultMessage = null
                    uploadCroppedImageForMatching(bitmap, "$baseUrl/match") { success, response ->
                        matching = false
                        resultMessage = if (success) {
                            "Match success: $response"
                        } else {
                            "Match failed: $response"
                        }
                    }
                },
                enabled = !uploading && !matching
            ) {
                Text(if (matching) "Matching..." else "Match")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Upload button
            Button(
                onClick = {
                    uploading = true
                    resultMessage = null
                    uploadCroppedImageWithName(bitmap, name, "$baseUrl/upload") { success, response ->
                        uploading = false
                        resultMessage = if (success) {
                            "Upload success: $response"
                        } else {
                            "Upload failed: $response"
                        }
                    }
                },
                enabled = name.isNotBlank() && !uploading && !matching
            ) {
                Text(if (uploading) "Uploading..." else "Upload")
            }

            if (resultMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(resultMessage ?: "", color = Color.DarkGray)
            }
        }
    }
}
