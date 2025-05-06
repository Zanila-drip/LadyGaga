package com.zanila.ladygaga


import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.widget.Toast
import androidx.annotation.RequiresPermission
import java.io.OutputStream
import java.util.*

class BluetoothManager(private val context: Context, private val macAddress: String) {

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun isAvailable(): Boolean = adapter != null
    fun isEnabled(): Boolean = adapter?.isEnabled == true

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val device: BluetoothDevice? = adapter?.bondedDevices?.find { it.address == macAddress }
        if (device == null) {
            Toast.makeText(context, "Dispositivo no emparejado", Toast.LENGTH_SHORT).show()
            return
        }

        Thread @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN) {
            try {
                adapter?.cancelDiscovery()
                socket = device.createRfcommSocketToServiceRecord(uuid)
                socket?.connect()
                outputStream = socket?.outputStream
                (context as? MainActivity)?.runOnUiThread { onSuccess() }
            } catch (e: Exception) {
                (context as? MainActivity)?.runOnUiThread { onError(e) }
            }
        }.start()
    }

    fun send(command: String) {
        try {
            outputStream?.write(command.toByteArray())
        } catch (e: Exception) {
            Toast.makeText(context, "Error al enviar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun disconnect() {
        socket?.close()
    }
}