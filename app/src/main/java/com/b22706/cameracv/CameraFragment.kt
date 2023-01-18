package com.b22706.cameracv

import android.Manifest
import androidx.camera.lifecycle.ProcessCameraProvider
import android.os.Bundle
import android.util.Size
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.b22706.cameracv.databinding.FragmentCameraBinding
import com.google.common.util.concurrent.ListenableFuture
import pub.devrel.easypermissions.EasyPermissions

class CameraFragment : Fragment() {
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>

    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())

        val permissions = arrayOf(
                Manifest.permission.CAMERA
            )

        if (!EasyPermissions.hasPermissions(requireActivity(), *permissions)) {
            // パーミッションが許可されていない時の処理
            EasyPermissions.requestPermissions(requireActivity(), "パーミッションに関する説明", 0, *permissions)
            return
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(requireActivity()))
    }

    private fun bindPreview(cameraProvider : ProcessCameraProvider) {
        val preview : Preview = Preview.Builder()
            .build()

        val cameraSelector : CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(binding.previewView.surfaceProvider)

        val imageCapture = ImageCapture.Builder()
            .setTargetRotation(requireView().display.rotation)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            // RGBA出力が必要な場合は、以下の行を有効にしてください
            // .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

//        imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { imageProxy ->
//            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
//            // insert your code here.
//
//            // after done, release the ImageProxy object
//            imageProxy.close()
//        })

        var camera = cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            imageCapture,
            imageAnalysis,
            preview
        )
    }


    companion object {
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CameraFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}