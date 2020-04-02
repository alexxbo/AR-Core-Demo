package com.aborovskoy.arcoredemo

import android.os.Bundle
import android.os.Handler
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aborovskoy.arcoredemo.common.hasCameraPermission
import com.aborovskoy.arcoredemo.common.launchPermissionSettings
import com.aborovskoy.arcoredemo.common.requestCameraPermission
import com.aborovskoy.arcoredemo.common.shouldShowRequestRationale
import com.google.ar.core.ArCoreApk
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {

    // Set to true ensures requestInstall() triggers installation if necessary.
    private var userRequestedInstall = true
    private var session: Session? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enable AR related functionality on ARCore supported devices only.
        maybeEnableArButton()

        privacyPolicy.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onResume() {
        super.onResume()

        if (session == null) {

            // Make sure Google Play Services for AR is installed and up to date.
            try {
                ArCoreApk.getInstance()?.requestInstall(this, userRequestedInstall)?.let { request ->
                    when (request) {
                        ArCoreApk.InstallStatus.INSTALLED -> {
                            // ARCore requires camera permission to operate.
                            if (!hasCameraPermission(this)) {
                                requestCameraPermission(this)
                                return
                            }

                            // Success, create the AR session.
                            session = Session(this)
                            configCamera(session!!)
                        }
                        ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                            // Ensures next invocation of requestInstall() will either return
                            // INSTALLED or throw an exception.
                            userRequestedInstall = false
                            return
                        }
                    }
                }
            } catch (error: Exception) {
                onError(error)
                return
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session?.resume()
        } catch (error: CameraNotAvailableException) {
            onError(error)
            session = null
            return
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (!hasCameraPermission(this)) {
            Toast.makeText(this, getString(R.string.camera_permission_description), Toast.LENGTH_LONG).show()

            if (!shouldShowRequestRationale(this)) {
                // Permission denied with checking "Do not ask again".
                launchPermissionSettings(this)
            }
            finish()
        }
    }

    private fun maybeEnableArButton() {
        val availability: ArCoreApk.Availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            // Re-query at 5Hz while compatibility is checked in the background.
            Handler().postDelayed({ maybeEnableArButton() }, 200)
        }

        if (availability.isSupported) {
            arButton.visibility = View.VISIBLE
            arButton.isEnabled = true
            // indicator on the button.
        } else { // Unsupported or unknown.
            arButton.visibility = View.INVISIBLE
            arButton.isEnabled = false
        }
    }

    private fun configCamera(session: Session) {
        // Create a camera config filter for the session.
        val filter = CameraConfigFilter(session)

        // Return only camera configs that target 30 fps camera capture frame rate.
        filter.setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30))

        // Return only camera configs that will not use the depth sensor.
        filter.setDepthSensorUsage(EnumSet.of(CameraConfig.DepthSensorUsage.DO_NOT_USE))

        // Get list of configs that match filter settings.
        // In this case, this list is guaranteed to contain at least one element,
        // because both TargetFps.TARGET_FPS_30 and DepthSensorUsage.DO_NOT_USE
        // are supported on all ARCore supported devices.
        val cameraConfigList = session.getSupportedCameraConfigs(filter)

        // Use element 0 from the list of returned camera configs. This is because
        // it contains the camera config that best matches the specified filter
        // settings.
        session.cameraConfig = cameraConfigList[0]
    }

    private fun onError(error: Exception) {
        val message = when (error) {
            is CameraNotAvailableException -> R.string.error_camera_message
            is UnavailableArcoreNotInstalledException -> R.string.error_install_ar_core_message
            is UnavailableUserDeclinedInstallationException -> R.string.error_install_ar_core_message
            is UnavailableApkTooOldException -> R.string.error_update_ar_core_message
            is UnavailableSdkTooOldException -> R.string.error_update_app_message
            is UnavailableDeviceNotCompatibleException -> R.string.error_not_supported_ar_message
            else -> R.string.error_create_session_message
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e(MainActivity::class.java.simpleName, "Exception creating session", error)
    }
}