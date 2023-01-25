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
import org.opencv.imgproc.Imgproc
import java.util.concurrent.TimeUnit


class Distortion(): ImageAnalysis.Analyzer {

    companion object {
        val LOG_NAME: String = "Distortion"
    }

    private var matPrevious: Mat? = null

    // 出力する画像
    private val _image =
        MutableLiveData<Bitmap>(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
    val image: LiveData<Bitmap> = _image

    // ここに毎フレーム画像が渡される

    private var lastAnalyzedTimestamp = 0L
    override fun analyze(image: ImageProxy) {

        val currentTimestamp = System.currentTimeMillis()
        // Analyze only if 1 second has passed since the last analysis
        if (currentTimestamp - lastAnalyzedTimestamp >= TimeUnit.MILLISECONDS.toMillis(100)) {
            val mat = imageProxyToMat(image)
            val rMat = fixMatRotation(mat)
            val bitmap = rMat.toBitmap()

            _image.postValue(bitmap)
            lastAnalyzedTimestamp = currentTimestamp
        }

        // close()しないと次の画像がこない
        image.close()
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

    fun imageProxyToMat(image: ImageProxy): Mat {
        Log.d(LOG_NAME,"${image.format}")
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

    fun Mat.toBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(cols(), rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(this, bmp)
        return bmp
    }

}