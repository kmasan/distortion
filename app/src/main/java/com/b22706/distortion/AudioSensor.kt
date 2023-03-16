package com.b22706.distortion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import org.jtransforms.fft.DoubleFFT_1D
import java.util.stream.IntStream
import kotlin.math.log10
import kotlin.math.sqrt


class AudioSensor(private val context: Context) {

    companion object {
        const val LOG_NAME: String = "AudioSensor"
        const val RECORDING_NORMAL = 0 // 特に処理はしない
        const val RECORDING_DB = 1 // volumeに音量を記載
        const val RECORDING_FREQUENCY = 2 // 周波数解析
        const val RECORDING_DB_AND_FREQUENCY = 3 // 周波数解析
    }

    private val sampleRate = 8000 // 標準：44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    lateinit var audioRecord: AudioRecord
    private var buffer:ShortArray = ShortArray(bufferSize)
    fun getBuffer() = buffer

    private var isRecoding: Boolean = false
    private var run: Boolean = false

    var volume = 0
    private set
    var voice = 0
    private set


    // period: オーディオ処理のインターバル, recordingMode: 処理の種類（定数として宣言済み）
    fun start(period: Int, recordingMode: Int) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecord.startRecording()

        isRecoding = true
        if (!run) when(recordingMode){
            RECORDING_NORMAL -> recoding(period)
            RECORDING_DB -> recodingDB(period)
            RECORDING_FREQUENCY -> recodingFrequency(period)
            RECORDING_DB_AND_FREQUENCY -> recodingDBAndFrequency(period)
        }
    }

    // 8000Hzでこの処理を回すのはやばいんで指定period[ms]ごとに処理
    private fun recoding(period: Int) {
        val hnd0 = Handler(Looper.getMainLooper())
        run = true
        // こいつ(rnb0) が何回も呼ばれる
        val rnb0 = object : Runnable {
            override fun run() {
                // こっから自由
                // bufferReadResultは使わない．bufferにデータが入るのでこれを使う
                var bufferReadResult = audioRecord.read(buffer,0,bufferSize)
                // 振幅が出る
                Log.d(LOG_NAME,"${buffer[100]}, ${buffer[300]}, ${buffer.size}")

                // stop用のフラグ
                if (run) {
                    // 指定時間後に自分自身を呼ぶ
                    hnd0.postDelayed(this, period.toLong())
                }
            }
        }
        // 初回の呼び出し
        hnd0.post(rnb0)
    }

    // デシベル変換したやつを出力
    private fun recodingDB(period: Int) {
        val hnd0 = Handler(Looper.getMainLooper())
        run = true
        val rnb0 = object : Runnable {
            override fun run() {
                var bufferReadResult = audioRecord.read(buffer,0,bufferSize)

                // 最大音量を解析
                val sum = buffer.sumOf { it.toDouble() * it.toDouble() }
                val amplitude = sqrt(sum / bufferSize)
                // デシベル変換
                val db = (20.0 * log10(amplitude)).toInt()
                volume = db
                //Log.d(LOG_NAME,"db = $db")

                if (run) {
                    hnd0.postDelayed(this, period.toLong())
                }
            }
        }
        hnd0.post(rnb0)
    }

    // 最大音量の周波数を出力
    private fun recodingFrequency(period: Int){
        val hnd0 = Handler(Looper.getMainLooper())
        run = true
        // こいつ(rnb0) が何回も呼ばれる
        val rnb0 = object : Runnable {
            override fun run() {
                // bufferにデータが入る
                audioRecord.read(buffer,0,bufferSize)
                // 振幅が出る
                //Log.d(LOG_NAME,"${buffer[100]}, ${buffer[300]}, ${buffer.size}")

                // FFT 結果はfftBufferに入る
                val fft = DoubleFFT_1D(bufferSize.toLong())
                val fftBuffer = DoubleArray(bufferSize * 2)
                val doubleBuffer: DoubleArray = buffer.map { it.toDouble() }.toDoubleArray()
                System.arraycopy(doubleBuffer, 0, fftBuffer, 0, bufferSize)
                fft.realForward(fftBuffer)

                // Log.d(LOG_NAME, "${fftBuffer.toList()}")
                // Log.d(LOG_NAME, "${fftBuffer.size}")

                //解析
//                val targetFrequency = 10000 // 特定の周波数（Hz）
//                val index = (targetFrequency * fft.size / sampleRate).toInt()
//                val amplitude = sqrt((fft[index] * fft[index] + fft[index + 1] * fft[index + 1]).toDouble())
                //振幅が最大の周波数とその振幅値の解析
                var maxAmplitude = 0.0 // 最大振幅
                var maxIndex = 0 // 最大振幅が入っているリスト番号
                // 最大振幅が入っているリスト番号を走査
                for(index in IntStream.range(0, fftBuffer.size - 1)){
                    val tmp = sqrt((fftBuffer[index] * fftBuffer[index] + fftBuffer[index + 1] * fftBuffer[index + 1]))
                    if (maxAmplitude < tmp){
                        maxAmplitude = tmp
                        maxIndex = index
                    }
                }
                // 最大振幅の周波数
                val maxFrequency: Int = (maxIndex * sampleRate / fftBuffer.size)
                Log.d(LOG_NAME, "maxFrequency = $maxFrequency")

                // 最大振幅の周波数をレベルに変換（0~10）
                val minVoice = 100 // レベル1とする最低周波数
                val maxVoice = 1000 // レベル10とする最大周波数
                val levelInterval = (maxVoice - minVoice)/10 // レベルの間隔
                val voiceLevel: Int = (maxFrequency - minVoice)/ levelInterval // レベルに変換
                voice = when{
                    voiceLevel < 0 -> 0
                    voiceLevel > 10 -> 10
                    else -> voiceLevel
                }

                // stop用のフラグ
                if (run) {
                    // 指定時間後に自分自身を呼ぶ
                    hnd0.postDelayed(this, period.toLong())
                }
            }
        }
        // 初回の呼び出し
        hnd0.post(rnb0)
    }

    // デシベル変換したやつを出力
    private fun recodingDBAndFrequency(period: Int) {
        val hnd0 = Handler(Looper.getMainLooper())
        run = true
        val rnb0 = object : Runnable {
            override fun run() {
                var bufferReadResult = audioRecord.read(buffer,0,bufferSize)

                // 最大音量を解析
                val sum = buffer.sumOf { it.toDouble() * it.toDouble() }
                val amplitude = sqrt(sum / bufferSize)
                // デシベル変換
                val db = (20.0 * log10(amplitude)).toInt()
                volume = db
                //Log.d(LOG_NAME,"db = $db")

                // 最大の周波数解析
                // FFT 結果はfftBufferに入る
                val fft = DoubleFFT_1D(bufferSize.toLong())
                val fftBuffer = DoubleArray(bufferSize * 2)
                val doubleBuffer: DoubleArray = buffer.map { it.toDouble() }.toDoubleArray()
                System.arraycopy(doubleBuffer, 0, fftBuffer, 0, bufferSize)
                fft.realForward(fftBuffer)

                //振幅が最大の周波数とその振幅値の解析
                var maxAmplitude = 0.0 // 最大振幅
                var maxIndex = 0 // 最大振幅が入っているリスト番号
                // 最大振幅が入っているリスト番号を走査
                for(index in IntStream.range(0, fftBuffer.size - 1)){
                    val tmp = sqrt((fftBuffer[index] * fftBuffer[index] + fftBuffer[index + 1] * fftBuffer[index + 1]))
                    if (maxAmplitude < tmp){
                        maxAmplitude = tmp
                        maxIndex = index
                    }
                }
                // 最大振幅の周波数
                val maxFrequency: Int = (maxIndex * sampleRate / fftBuffer.size)
                Log.d(LOG_NAME, "amplitude = $amplitude, $maxAmplitude maxFrequency = $maxFrequency")

                if (run) {
                    hnd0.postDelayed(this, period.toLong())
                }
            }
        }
        hnd0.post(rnb0)
    }

    fun stop() {
        run = false
    }
}