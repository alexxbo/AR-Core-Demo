package com.aborovskoy.arcoredemo

import android.os.Bundle
import android.os.Handler
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aborovskoy.arcoredemo.common.hasCameraPermission
import com.aborovskoy.arcoredemo.common.launchPermissionSettings
import com.aborovskoy.arcoredemo.common.requestCameraPermission
import com.aborovskoy.arcoredemo.common.shouldShowRequestRationale
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import kotlinx.android.synthetic.main.activity_main.*


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

        // ARCore requires camera permission to operate.
        if (!hasCameraPermission(this)) {
            requestCameraPermission(this)
            return
        }

        // Make sure Google Play Services for AR is installed and up to date.
        try {
            if (session == null) {
                ArCoreApk.getInstance()?.requestInstall(this, userRequestedInstall)?.let { request ->
                    when (request) {
                        ArCoreApk.InstallStatus.INSTALLED ->
                            // Success, create the AR session.
                            session = Session(this)
                        ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                            // Ensures next invocation of requestInstall() will either return
                            // INSTALLED or throw an exception.
                            userRequestedInstall = false
                            return
                        }
                    }
                }
            }
        } catch (error: UnavailableUserDeclinedInstallationException) {
            // Display an appropriate message to the user and return gracefully.
            Toast.makeText(this, "TODO: handle exception $error}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
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
}
