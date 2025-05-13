package com.example.thumbrecognizer

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.thumbrecognizer.models.MatchResponse
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
fun cropBitmapFromCircle(
    original: Bitmap,
    displayedImageSize: IntSize,
    imageOffset: Offset,
    squareTopLeft: Offset,
    squareSize: Float
): Bitmap {
    val relativeX = squareTopLeft.x - imageOffset.x
    val relativeY = squareTopLeft.y - imageOffset.y

    val scaleX = original.width.toFloat() / displayedImageSize.width
    val scaleY = original.height.toFloat() / displayedImageSize.height

    val scaledLeft = (relativeX * scaleX).toInt().coerceAtLeast(0)
    val scaledTop = (relativeY * scaleY).toInt().coerceAtLeast(0)
    val scaledSizeX = (squareSize * scaleX).toInt()
    val scaledSizeY = (squareSize * scaleY).toInt()

    val cropped = Bitmap.createBitmap(
        original,
        scaledLeft,
        scaledTop,
        scaledSizeX.coerceAtMost(original.width - scaledLeft),
        scaledSizeY.coerceAtMost(original.height - scaledTop)
    )

    return cropped
}

fun uploadCroppedImageWithName(
    bitmap: Bitmap,
    name: String,
    uploadUrl: String,
    onResult: (Boolean, String?) -> Unit
) {
    val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS) // wait for connection
        .readTimeout(120, TimeUnit.SECONDS)   // wait for server response
        .writeTimeout(60, TimeUnit.SECONDS)   // wait for upload to finish
        .build()

    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
    val byteArray = stream.toByteArray()

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("name", name)
        .addFormDataPart(
            name = "file",
            filename = "cropped_image.jpg",
            body = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
        )
        .build()

    val request = Request.Builder()
        .url(uploadUrl)
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onResult(false, e.message)
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                onResult(true, response.body?.string())
            } else {
                onResult(false, "HTTP ${response.code}")
            }
        }
    })
}


fun uploadCroppedImageForMatching(
    bitmap: Bitmap,
    uploadUrl: String,
    onResult: (Boolean, MatchResponse?) -> Unit
) {
    val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
    val byteArray = stream.toByteArray()

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart(
            name = "file",
            filename = "image.jpg",
            body = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
        )
        .build()

    val request = Request.Builder()
        .url(uploadUrl)
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onResult(false, null)
        }

        override fun onResponse(call: Call, response: Response) {

            if (!response.isSuccessful) {
                onResult(false, null)
                return
            }

            val body = response.body?.string()
            if (body != null) {
                try {
                    val parsed = Gson().fromJson(body, MatchResponse::class.java)
                    onResult(true, parsed)
                } catch (_: Exception) {
                    onResult(false, null)
                }
            } else {
                onResult(false, null)
            }
        }
    })
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun CircularCropper(
    bitmap: Bitmap,
    onCropConfirmed: (Bitmap) -> Unit
) {

    val squareSize = remember { mutableFloatStateOf(300f) }
    var topLeft by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val imageWidth = bitmap.width
    val imageHeight = bitmap.height

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                canvasSize = it.size
                if (topLeft == Offset.Zero) {
                    val centerX = it.size.width / 2f - squareSize.floatValue / 2f
                    val centerY = it.size.height / 2f - squareSize.floatValue / 2f
                    topLeft = Offset(centerX, centerY)
                }
            }
    ) {
        val containerWidth = constraints.maxWidth
        val containerHeight = constraints.maxHeight
        val containerAspectRatio = containerWidth.toFloat() / containerHeight
        val bitmapAspectRatio = imageWidth.toFloat() / imageHeight

        val displayedImageSize: IntSize
        val imageOffset: Offset

        if (bitmapAspectRatio > containerAspectRatio) {
            val width = containerWidth
            val height = (width / bitmapAspectRatio).toInt()
            displayedImageSize = IntSize(width, height)
            imageOffset = Offset(0f, ((containerHeight - height) / 2f))
        } else {
            val height = containerHeight
            val width = (height * bitmapAspectRatio).toInt()
            displayedImageSize = IntSize(width, height)
            imageOffset = Offset(((containerWidth - width) / 2f), 0f)
        }

        Box(
            modifier = Modifier
                .size(
                    width = with(LocalDensity.current) { displayedImageSize.width.toDp() },
                    height = with(LocalDensity.current) { displayedImageSize.height.toDp() }
                )
                .offset {
                    IntOffset(imageOffset.x.toInt(), imageOffset.y.toInt())
                }
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        topLeft += dragAmount
                    }
                }
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(Color.Black.copy(alpha = 0.6f))

            drawRect(
                color = Color.Transparent,
                topLeft = topLeft,
                size = Size(squareSize.floatValue, squareSize.floatValue),
                blendMode = BlendMode.Clear
            )
        }

        Button(
            onClick = {
                val cropped = cropBitmapFromCircle(
                    original = bitmap,
                    displayedImageSize = displayedImageSize,
                    imageOffset = imageOffset,
                    squareTopLeft = topLeft,
                    squareSize = squareSize.floatValue
                )
                onCropConfirmed(cropped)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        ) {
            Text("Crop")
        }
    }
}