package com.b22706.distortion.ui

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.b22706.distortion.MainActivity
import com.b22706.distortion.databinding.FragmentCameraBinding
import pub.devrel.easypermissions.EasyPermissions


class CameraFragment : Fragment() {

    companion object {
        const val LOG_NAME = "CameraFragment"
    }

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var activity: MainActivity

    private val cameraViewModel: CameraViewModel by viewModels{
        CameraViewModelFactory((requireActivity() as MainActivity))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = (requireActivity() as MainActivity)
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraViewModel.startAudio()
        cameraViewModel.startCamera(this)

        cameraViewModel.distortion.image.observe(viewLifecycleOwner){
            activity.runOnUiThread {
                binding.imageView.setImageBitmap(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        cameraViewModel.startAudio()
    }

    override fun onPause() {
        super.onPause()
        cameraViewModel.distortion.audioSensor.stop()
    }
}