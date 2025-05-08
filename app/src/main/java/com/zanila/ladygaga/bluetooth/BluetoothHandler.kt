package com.zanila.ladygaga.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.UUID

class BluetoothHandler(private val context: Context) {
    private val TAG = "BluetoothHandler"
    private val HC05_ADDRESS = "98:D3:31:F5:B9:E7" // Reemplaza con la dirección MAC de tu HC-05
    private val UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB"
    private var bluetoothSocket: BluetoothSocket? = null
    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var dataReceivedListener: ((String) -> Unit)? = null

    fun setDataReceivedListener(listener: (String) -> Unit) {
        dataReceivedListener = listener
    }

    fun checkAndRequestPermissions(context: Context) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

        val permissionsToRequest = permissions.filter {
            ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(context as android.app.Activity, permissionsToRequest, 1)
        }
    }

    fun connectToHC05(onConnected: () -> Unit, onError: (String) -> Unit) {
        if (bluetoothAdapter == null) {
            onError("Bluetooth no está disponible en este dispositivo")
            return
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            onError("Se requieren permisos de Bluetooth")
            return
        }

        try {
            val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(HC05_ADDRESS)
            bluetoothSocket = device?.createRfcommSocketToServiceRecord(UUID.fromString(UUID_STRING))
            bluetoothSocket?.connect()
            onConnected()
            startListening()
        } catch (e: IOException) {
            Log.e(TAG, "Error al conectar: ${e.message}")
            onError("Error al conectar: ${e.message}")
            try {
                bluetoothSocket?.close()
            } catch (closeException: IOException) {
                Log.e(TAG, "Error al cerrar el socket: ${closeException.message}")
            }
        }
    }

    private fun startListening() {
        Thread {
            val buffer = ByteArray(1024)
            while (true) {
                try {
                    val bytes = bluetoothSocket?.inputStream?.read(buffer)
                    if (bytes != null && bytes > 0) {
                        val data = String(buffer, 0, bytes)
                        dataReceivedListener?.invoke(data)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error al leer datos: ${e.message}")
                    break
                }
            }
        }.start()
    }

    fun disconnect() {
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error al cerrar la conexión: ${e.message}")
        }
    }
} 