package com.zanila.ladygaga

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class BluetoothHandler(private val context: Context) {

    private val REQUEST_BLUETOOTH_PERMISSIONS = 2
    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    var outputStream: OutputStream? = null
    private val hc05MacAddress = "00:23:10:00:D3:38"
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    init {
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth no disponible", Toast.LENGTH_LONG).show()
        }
    }

    fun checkAndRequestPermissions(activity: MainActivity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                REQUEST_BLUETOOTH_PERMISSIONS
            )
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToHC05(onConnected: () -> Unit, onError: (String) -> Unit) {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val device: BluetoothDevice? = pairedDevices?.find { it.address == hc05MacAddress }

        if (device == null) {
            Toast.makeText(context, "No se encontró el HC-05 emparejado", Toast.LENGTH_SHORT).show()
            return
        }

        Thread @androidx.annotation.RequiresPermission(Manifest.permission.BLUETOOTH_SCAN) {
            bluetoothAdapter?.cancelDiscovery()
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(sppUuid)
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                (context as? MainActivity)?.runOnUiThread {
                    onConnected()
                }
            } catch (e: IOException) {
                (context as? MainActivity)?.runOnUiThread {
                    onError("Error al conectar: ${e.message}")
                }
            }
        }.start()
    }

    fun disconnect() {
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            // Manejar la excepción al cerrar el socket
            (context as? MainActivity)?.runOnUiThread {
                Toast.makeText(context, "Error al desconectar Bluetooth: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } finally {
            bluetoothSocket = null
            outputStream = null
        }
    }

    fun sendData(data: String) {
        if (outputStream != null) {
            try {
                outputStream?.write(data.toByteArray())
                outputStream?.flush() // Asegurarse de que los datos se envíen inmediatamente
            } catch (e: IOException) {
                (context as? MainActivity)?.runOnUiThread {
                    Toast.makeText(context, "Error al enviar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            (context as? MainActivity)?.runOnUiThread {
                Toast.makeText(context, "No se ha establecido la conexión Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }
    }
}