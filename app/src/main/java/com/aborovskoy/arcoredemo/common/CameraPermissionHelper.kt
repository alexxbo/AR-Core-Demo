package com.aborovskoy.arcoredemo.common

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private const val CAMERA_PERMISSION_CODE = 0
private const val CAMERA_PERMISSION = Manifest.permission.CAMERA

/** Check to see we have the necessary permissions for this app.  */
fun hasCameraPermission(activity: Activity): Boolean =
        (ContextCompat.checkSelfPermission(activity, CAMERA_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED)

/** Check to see we have the necessary permissions for this app, and ask for them if we don't.  */
fun requestCameraPermission(activity: Activity) {
    ActivityCompat.requestPermissions(activity, arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSION_CODE)
}

/** Check to see if we need to show the rationale for this permission.  */
fun shouldShowRequestRationale(activity: Activity): Boolean =
        ActivityCompat.shouldShowRequestPermissionRationale(activity, CAMERA_PERMISSION)


/** Launch Application Setting to grant permission.  */
fun launchPermissionSettings(activity: Activity) {
    Intent().apply {
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        data = Uri.fromParts("package", activity.packageName, null)
        activity.startActivity(this)
    }
}