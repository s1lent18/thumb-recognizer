package com.example.thumbrecognizer

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.min

fun cropBitmapFromCircle(
    original: Bitmap,
    displayedImageSize: IntSize,
    imageOffset: Offset,
    circleCenter: Offset,
    radius: Float
): Bitmap {
    // Adjust crop center relative to image area (excluding padding)
    val relativeX = circleCenter.x - imageOffset.x
    val relativeY = circleCenter.y - imageOffset.y

    // Scale relative coordinates to bitmap
    val scaleX = original.width.toFloat() / displayedImageSize.width
    val scaleY = original.height.toFloat() / displayedImageSize.height

    val scaledCenterX = relativeX * scaleX
    val scaledCenterY = relativeY * scaleY
    val scaledRadiusX = radius * scaleX
    val scaledRadiusY = radius * scaleY

    val left = (scaledCenterX - scaledRadiusX).toInt().coerceAtLeast(0)
    val top = (scaledCenterY - scaledRadiusY).toInt().coerceAtLeast(0)
    val diameter = (2 * min(scaledRadiusX, scaledRadiusY)).toInt()

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

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun CircularCropper(
    bitmap: Bitmap,
    onCropConfirmed: (Bitmap) -> Unit
) {
    val imageWidth = bitmap.width
    val imageHeight = bitmap.height

    val circleRadius = remember { mutableFloatStateOf(300f) }
    var circleCenter by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                canvasSize = it.size
                if (circleCenter == Offset.Zero) {
                    circleCenter = Offset(it.size.width / 2f, it.size.height / 2f)
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
            // Image fills width
            val width = containerWidth
            val height = (width / bitmapAspectRatio).toInt()
            displayedImageSize = IntSize(width, height)
            imageOffset = Offset(0f, ((containerHeight - height) / 2f))
        } else {
            // Image fills height
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

        // Transparent circle overlay with drag
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
                    displayedImageSize = displayedImageSize,
                    imageOffset = imageOffset,
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