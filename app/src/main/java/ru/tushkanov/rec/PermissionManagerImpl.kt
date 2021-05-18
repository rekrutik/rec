package ru.tushkanov.rec

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference

class PermissionManagerImpl(activity: Activity) : PermissionManager {

    private val activityRef = WeakReference(activity)

    fun openSettings() {
        val activity = activityRef.get()!!
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivity(intent)
    }

    override fun requestPermission(permission: String) {
        ActivityCompat.requestPermissions(activityRef.get()!!, arrayOf(permission), 228)
    }

    override fun checkPermission(permission: String) : PermissionManager.PermissionState {
        val activity = activityRef.get()!!
        val result = ContextCompat.checkSelfPermission(activity, permission)

        val granted = result == PackageManager.PERMISSION_GRANTED
        val blacklisted = !granted && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        return if (granted) PermissionManager.PermissionState.ALLOWED else if (blacklisted) PermissionManager.PermissionState.BLACKLISTED else PermissionManager.PermissionState.DENIED
    }
}