package com.b22706.distortion

import android.app.Application
import android.util.Log
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat


class MainApplication: Application() {

    lateinit var audioSensor: AudioSensor

    override fun onCreate() {
        super.onCreate()

        audioSensor = AudioSensor(applicationContext)
        // audioSensor.start()

    }

}