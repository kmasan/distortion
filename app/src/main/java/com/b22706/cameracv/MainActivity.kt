package com.b22706.cameracv

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import org.opencv.android.OpenCVLoader

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("opencv", OpenCVLoader.OPENCV_VERSION)
    }
}