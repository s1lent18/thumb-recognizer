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
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.thumbrecognizer.models.MatchResponse

@Composable
fun FinalPhotoScreen(
    bitmap: Bitmap,
    baseUrl: String = "http://172.16.85.93:8080"
) {
    var name by remember { mutableStateOf("") }
    var uploading by remember { mutableStateOf(false) }
    var matching by remember { mutableStateOf(false) }
    var matchResult by remember { mutableStateOf<MatchResponse?>(null) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }

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
                    //.clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name for Upload") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    matching = true
                    matchResult = null
                    uploadMessage = null
                    uploadCroppedImageForMatching(bitmap, "$baseUrl/match") { success, response ->
                        matching = false
                        if (success && response != null) {
                            matchResult = response
                        } else {
                            uploadMessage = "Matching failed"
                        }
                    }
                },
                enabled = !uploading && !matching
            ) {
                if (matching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Match")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    uploading = true
                    matchResult = null
                    uploadMessage = null
                    uploadCroppedImageWithName(bitmap, name, "$baseUrl/upload") { success, response ->
                        uploading = false
                        uploadMessage = if (success) {
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

            Spacer(modifier = Modifier.height(16.dp))

            matchResult?.let { result ->
                Text("Matches:")
                Spacer(modifier = Modifier.height(8.dp))
                result.matches.forEach {
                    Text("• ${it.name} (similarity: ${"%.2f".format(it.similarity)})")
                }
            }

            uploadMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = Color.DarkGray)
            }
        }
    }
}