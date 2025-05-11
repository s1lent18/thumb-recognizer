package com.example.thumbrecognizer

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    private val _bitmap = MutableStateFlow<Bitmap?>(null)
    val bitmap = _bitmap.asStateFlow()

    private val _croppedBitmap = MutableStateFlow<Bitmap?>(null)
    val croppedBitmap = _croppedBitmap.asStateFlow()

    fun onPhotoTaken(bitmap: Bitmap) {
        _bitmap.value = bitmap
    }

    fun clearPhoto() {
        _bitmap.value = null
        _croppedBitmap.value = null
    }

    fun onCropConfirmed(bitmap: Bitmap) {
        _croppedBitmap.value = bitmap
    }
}