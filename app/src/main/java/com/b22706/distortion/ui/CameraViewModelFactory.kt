package com.b22706.distortion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.b22706.distortion.MainActivity

class CameraViewModelFactory(private val activity: MainActivity): ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CameraViewModel(activity) as T
        }
        throw IllegalAccessException("unk class")
    }
}