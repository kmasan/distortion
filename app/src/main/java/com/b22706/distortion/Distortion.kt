package com.b22706.distortion

import android.graphics.Bitmap
import android.graphics.Point
import android.util.Log
import android.view.Surface
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer


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
    override fun analyze(image: ImageProxy) {
        val matOrg: Mat = getMatFromImage(image)
        val mat = fixMatRotation(matOrg)
        val matOutput: Mat = Mat(mat!!.rows(), mat.cols(), mat.type())

        if (matPrevious == null) matPrevious = mat
        Core.absdiff(mat, matPrevious, matOutput)
        matPrevious = mat

        val rect: Rect =  Rect(10, 10, 100, 100)
        val point: Point = Point(10, 10)

        Imgproc.rectangle(matOutput, Rect(10, 10, 100, 100), Scalar(255.0, 0.0, 0.0))

        val bitmap = Bitmap.createBitmap(matOutput.cols(), matOutput.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(matOutput, bitmap)
        Log.d(LOG_NAME, "row ${matOrg.rows()}, cols ${matOrg.cols()}")
        Log.d(LOG_NAME, "row ${image.height}, cols ${image.width}")
        Log.d(LOG_NAME, "row ${bitmap.height}, cols ${bitmap.width}")
        _image.postValue(bitmap)
        image.close()
    }

    // image to Mat
    private fun getMatFromImage(image: ImageProxy): Mat {
        /* https://stackoverflow.com/questions/30510928/convert-android-camera2-api-yuv-420-888-to-rgb */
        val yBuffer: ByteBuffer = image.planes[0].buffer
        val uBuffer: ByteBuffer = image.planes[1].buffer
        val vBuffer: ByteBuffer = image.planes[2].buffer
        val ySize: Int = yBuffer.remaining()
        val uSize: Int = uBuffer.remaining()
        val vSize: Int = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        val yuv = Mat(image.height + image.height / 2, image.width, CvType.CV_8UC1)
        yuv.put(0, 0, nv21)
        val mat = Mat()
        Imgproc.cvtColor(yuv, mat, Imgproc.COLOR_YUV2RGB_NV21, 3)
        return mat
    }

    private fun fixMatRotation(matOrg: Mat): Mat? {
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

}