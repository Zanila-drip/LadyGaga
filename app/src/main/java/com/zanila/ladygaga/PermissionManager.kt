package com.zanila.ladygaga

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat

class PermissionManager(private var context: Context) {
    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.RECORD_AUDIO,

            )
    } else {
        TODO("VERSION.SDK_INT < S")
    }


    private val PERMISSION_GRANTED = PackageManager.PERMISSION_GRANTED

    fun requestPermissions() {
        for (permission in permissions) {
            if(ActivityCompat.checkSelfPermission(context, permission) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context as Activity, permissions, 5)
            }
            else {
                Log.d("PERMISSIONS", "Permission $permission Granted")
            }
        }
    }

}