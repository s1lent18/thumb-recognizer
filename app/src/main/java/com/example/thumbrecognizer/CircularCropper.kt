package com.example.thumbrecognizer

import android.graphics.Bitmap
//import android.graphics.BlendMode
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
import android.graphics.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

fun cropToCircle(original: Bitmap): Bitmap {
    val size = minOf(original.width, original.height)
    val x = (original.width - size) / 2
    val y = (original.height - size) / 2
    val squareBitmap = Bitmap.createBitmap(original, x, y, size, size)

    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(squareBitmap, 0f, 0f, paint)

    return output
}

fun cropBitmapFromCircle(
    original: Bitmap,
    canvasSize: IntSize,
    circleCenter: Offset,
    radius: Float
): Bitmap {
    val scaleX = original.width / canvasSize.width.toFloat()
    val scaleY = original.height / canvasSize.height.toFloat()

    // Scale crop area back to bitmap coordinates
    val scaledCenterX = circleCenter.x * scaleX
    val scaledCenterY = circleCenter.y * scaleY
    val scaledRadiusX = radius * scaleX
    val scaledRadiusY = radius * scaleY

    val left = (scaledCenterX - scaledRadiusX).toInt().coerceAtLeast(0)
    val top = (scaledCenterY - scaledRadiusY).toInt().coerceAtLeast(0)
    val diameter = (2 * minOf(scaledRadiusX, scaledRadiusY)).toInt()

    val squareBitmap = Bitmap.createBitmap(
        original,
        left,
        top,
        diameter.coerceAtMost(original.width - left),
        diameter.coerceAtMost(original.height - top)
    )

    val output = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    canvas.drawCircle(diameter / 2f, diameter / 2f, diameter / 2f, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(squareBitmap, 0f, 0f, paint)

    return output
}


@Composable
fun CircularCropper(
    bitmap: Bitmap,
    onCropConfirmed: (Bitmap) -> Unit
) {
    val imageWidth = bitmap.width
    val imageHeight = bitmap.height

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var circleCenter by remember {
        mutableStateOf(Offset.Zero)
    }
    val circleRadius = remember { mutableFloatStateOf(300f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                canvasSize = it.size
                if (circleCenter == Offset.Zero) {
                    circleCenter = Offset(it.size.width / 2f, it.size.height / 2f)
                }
            }
    ) {
        // Display the image
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay with transparent circle
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        circleCenter += dragAmount
                    }
                }
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                }
        ) {
            drawRect(Color.Black.copy(alpha = 0.6f))
            drawCircle(
                color = Color.Transparent,
                radius = circleRadius.floatValue,
                center = circleCenter,
                blendMode = BlendMode.Clear
            )
        }

        // Confirm button
        Button(
            onClick = {
                val cropped = cropBitmapFromCircle(
                    original = bitmap,
                    canvasSize = canvasSize,
                    circleCenter = circleCenter,
                    radius = circleRadius.floatValue
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