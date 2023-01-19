package com.b22706.distortion.ui

import android.Manifest
import android.R.attr.bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.b22706.distortion.MainApplication
import com.b22706.distortion.databinding.FragmentCameraBinding
import pub.devrel.easypermissions.EasyPermissions


class CameraFragment : Fragment() {

    companion object {
        val LOG_NAME = "CameraFragment"
    }

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var app: MainApplication

    private val cameraViewModel: CameraViewModel by viewModels{
        CameraViewModelFactory((requireActivity().application as MainApplication))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        app = (requireActivity().application as MainApplication)
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraViewModel.startCamera(this)

        cameraViewModel.distortion.image.observe(viewLifecycleOwner){
            activity?.runOnUiThread(Runnable {
                Log.d(LOG_NAME,"a")
                binding.imageView.setImageBitmap(it)
            })
        }

    }
}