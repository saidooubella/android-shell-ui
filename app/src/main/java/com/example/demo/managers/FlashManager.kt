@file:Suppress("DEPRECATION")

package com.example.demo.managers

import android.content.Context
import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.RequiresApi

internal abstract class FlashManager(
    internal val FacingFront: Int,
    internal val FacingBack: Int,
    internal val NeedsPermission: Boolean,
) {

    internal abstract fun setTorchMode(facing: Int, enabled: Boolean): Result

    internal abstract fun onCleared()

    @RequiresApi(Build.VERSION_CODES.M)
    private class FlashManagerMarshmallowImpl(
        private val manager: CameraManager,
    ) : FlashManager(
        CameraCharacteristics.LENS_FACING_FRONT,
        CameraCharacteristics.LENS_FACING_BACK,
        false
    ) {

        override fun setTorchMode(facing: Int, enabled: Boolean): Result {

            val cameraId = manager.cameraIdList.find { cameraId ->
                manager.getCameraCharacteristics(cameraId)[CameraCharacteristics.LENS_FACING] == facing
            } ?: return Result.CameraNotFound

            if (manager.getCameraCharacteristics(cameraId)[CameraCharacteristics.FLASH_INFO_AVAILABLE] == false) {
                return Result.FlashNotFound
            }

            manager.setTorchMode(cameraId, enabled)
            return Result.Success
        }

        override fun onCleared() = Unit
    }

    private class FlashManagerLollipopImpl : FlashManager(
        Camera.CameraInfo.CAMERA_FACING_FRONT,
        Camera.CameraInfo.CAMERA_FACING_BACK,
        true
    ) {

        private var camera: Camera? = null
        private var facing: Int = -1

        override fun setTorchMode(facing: Int, enabled: Boolean): Result {

            if (this.facing != facing || this.camera == null) {
                releaseCamera()
                camera = openCamera(facing)
                val camera = camera ?: return Result.CameraNotFound
                if (Camera.Parameters.FLASH_MODE_TORCH !in camera.parameters.supportedFlashModes) {
                    releaseCamera()
                    return Result.FlashNotFound
                }
            }

            camera?.applyParameters {
                flashMode =
                    if (enabled) Camera.Parameters.FLASH_MODE_TORCH else Camera.Parameters.FLASH_MODE_OFF
            }

            return Result.Success
        }

        override fun onCleared() = releaseCamera()

        private fun releaseCamera() {
            camera?.release()
            camera = null
            facing = -1
        }

        private fun openCamera(facing: Int): Camera? {
            val cameraInfo = Camera.CameraInfo()
            repeat(Camera.getNumberOfCameras()) { cameraId ->
                Camera.getCameraInfo(cameraId, cameraInfo)
                if (cameraInfo.facing == facing) {
                    return Camera.open(cameraId)
                }
            }
            return null
        }

        private fun Camera.applyParameters(block: Camera.Parameters.() -> Unit) {
            parameters = parameters.apply(block)
        }
    }

    enum class Result { CameraNotFound, FlashNotFound, Success }

    companion object {
        fun of(context: Context) = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE)
                FlashManagerMarshmallowImpl(cameraManager as CameraManager)
            }
            else -> FlashManagerLollipopImpl()
        }
    }
}
