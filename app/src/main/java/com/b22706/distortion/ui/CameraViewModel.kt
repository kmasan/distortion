package com.b22706.distortion.ui

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.b22706.distortion.Distortion
import com.b22706.distortion.MainApplication
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraViewModel(application: MainApplication) : ViewModel() {

    companion object {
        val LOG_NAME: String = "CameraViewModel"
    }
    val app: MainApplication = application

    val distortion: Distortion = Distortion()

    var width: Int = 1920
    var height: Int = 1444

    fun startCamera(fragment: Fragment) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(app)
        val context: Context = app.applicationContext

        fragment.view?.post {
            width = fragment.requireView().width
            height = fragment.requireView().height
            Log.d(LOG_NAME,"w=$width, h=$height")
        }

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()

                // 各フレームを解析できるAnalysis
                val imageAnalysis = ImageAnalysis.Builder()
                    // RGBA出力が必要な場合は、以下の行を有効にしてください
                    // .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setTargetResolution(Size(width, height))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
                // distortionに画像を送りなさい．
                imageAnalysis.setAnalyzer(cameraExecutor, distortion)

                // 今回使用するカメラは背面
                val cameraSelector =
                    CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()
                cameraProvider.unbindAll()

                // これらの設定を使ってLifecycle化
                val camera = cameraProvider.bindToLifecycle(
                    (fragment as LifecycleOwner),
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(LOG_NAME, "[startCamera] Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}