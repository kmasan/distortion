package com.b22706.distortion

import android.graphics.*
import android.util.Log
import android.view.Surface
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.sin


class Distortion(val audioSensor: AudioSensor): ImageAnalysis.Analyzer {

    companion object {
        val LOG_NAME: String = "Distortion"
        const val volumeThreshold: Int = 70
    }

    // 出力する画像
    private val _image =
        MutableLiveData<Bitmap>(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
    val image: LiveData<Bitmap> = _image
    private var willDistortion: Boolean = true
    private var timeStamp = System.currentTimeMillis()

    // ここに毎フレーム画像が渡される
    override fun analyze(image: ImageProxy) {
        try {
            val mat = imageProxyToMat(image)
            val rMat = fixMatRotation(mat)
//            Log.d(LOG_NAME,"audio db = ${audioSensor.getVolume()}")
//            Log.d(LOG_NAME, "distortionLevel = ${getDistortionLevel(audioSensor.getVolume())}")
            // 音量によって画像処理，音が一定以下なら何もしない．
            shouldDistortImage(rMat.clone())

            val bitmap = rMat.toBitmap()
            _image.postValue(bitmap)
        } finally {
            image.close()
        }
    }

    private fun shouldDistortImage(mat: Mat) {
        val level = getDistortionLevel(audioSensor.getVolume())
        if (level != 0 && toggleBoolean()){
            thread {
                Log.d(LOG_NAME, "save image")
                val dstMat = distortImage(mat, level)
                Log.d(LOG_NAME, "created dstImage")
                ImageManager.saveImage(
                    "level${level}",
                    dstMat.toBitmap()
                )
            }
        }
    }

    private fun toggleBoolean(): Boolean {
        val now = System.currentTimeMillis()
        // 一定時間経過したら
        if (now - timeStamp > 1000) {
            willDistortion = true
            timeStamp = now
        }
        return if (willDistortion){
            willDistortion = false
            return true
        } else {
            return false
        }
    }

    private fun distortImage(image: Mat, strength: Int): Mat {
        Log.d(LOG_NAME, "start distort")
        val result = Mat()
        val mapX = Mat()
        val mapY = Mat()
        val size = Size(image.cols().toDouble(), image.rows().toDouble())

        mapX.create(size, CvType.CV_32FC1)
        mapY.create(size, CvType.CV_32FC1)

        Log.d(LOG_NAME, "start Double for loop")
        for (i in 0 until image.rows()) {
            for (j in 0 until image.cols()) {
                val x = j.toDouble()
                val y = i.toDouble()
                val dx = x + strength * sin(y/10.0)
                val dy = y + strength * sin(x/10.0)
                mapX.put(i, j, dx)
                mapY.put(i, j, dy)
            }
        }
        Log.d(LOG_NAME, "end Double for loop")

        Imgproc.remap(image, result, mapX, mapY, Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, Scalar.all(0.0))
        Log.d(LOG_NAME, "end distort")
        return result
    }


    fun getDistortionLevel(volume: Int) :Int {
        return if (volume <= volumeThreshold) { 0 }
        else { (volume-volumeThreshold)/3 }
    }

    private fun fixMatRotation(matOrg: Mat): Mat {
        val mat: Mat
        val rotation: Int = Surface.ROTATION_0
        when (rotation) {
            Surface.ROTATION_0 -> {
                mat = Mat(matOrg.cols(), matOrg.rows(), matOrg.type())
                Core.transpose(matOrg, mat)
                Core.flip(mat, mat, 1)
            }
            Surface.ROTATION_90 -> mat = matOrg
            Surface.ROTATION_270 -> {
                mat = matOrg
                Core.flip(mat, mat, -1)
            }
            else -> {
                mat = Mat(matOrg.cols(), matOrg.rows(), matOrg.type())
                Core.transpose(matOrg, mat)
                Core.flip(mat, mat, 1)
            }
        }
        return mat
    }

    private fun imageProxyToMat(image: ImageProxy): Mat {
        val yuvType = Imgproc.COLOR_YUV2BGR_NV21
        val mat = Mat(image.height + image.height / 2, image.width, CvType.CV_8UC1)
        val data = ByteArray(image.planes[0].buffer.capacity() + image.planes[1].buffer.capacity())
        image.planes[0].buffer.get(data, 0, image.planes[0].buffer.capacity())
        image.planes[1].buffer.get(data, image.planes[0].buffer.capacity(), image.planes[1].buffer.capacity())
        mat.put(0, 0, data)
        val matRGBA = Mat()
        Imgproc.cvtColor(mat, matRGBA, yuvType)
        return matRGBA
    }

    private fun Mat.toBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(cols(), rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(this, bmp)
        return bmp
    }

}