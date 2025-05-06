package com.zanila.ladygaga


import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
@SuppressLint("MissingPermission")
class BluetoothHandler(
    private val bluetoothAdapter: BluetoothAdapter,
    private val macAddress: String = "00:23:10:00:D3:38"
) : Thread() {

    private var bluetoothSocket: BluetoothSocket? = null
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream

    private fun createSocket(device: BluetoothDevice): BluetoothSocket {
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID estándar para SPP
        return device.createRfcommSocketToServiceRecord(uuid)
    }

    override fun run() {
        var bluetoothSocket: BluetoothSocket? = null
        val device = bluetoothAdapter.getRemoteDevice(macAddress)

        try {
            bluetoothSocket = createSocket(device)
            bluetoothSocket.connect()  // Intenta conectar al dispositivo

            if (bluetoothSocket.isConnected) {
                Log.d("Bluetooth", "Conexión exitosa con ${device.name}")
                // Aquí puedes inicializar los flujos de entrada y salida
                inputStream = bluetoothSocket.inputStream
                outputStream = bluetoothSocket.outputStream
            } else {
                Log.d("Bluetooth", "No se pudo conectar")
            }
        } catch (e: IOException) {
            Log.d("Bluetooth", "Error de conexión: ${e.message}")
            e.printStackTrace()
        }
    }


    fun sendToHC05(valToSend: String) {
        try {
            if (bluetoothSocket?.isConnected == true) {
                outputStream.write(valToSend.toByteArray())
                outputStream.flush()
                Log.d("BluetoothHandler", "Sent: $valToSend")
            } else {
                Log.e("BluetoothHandler", "BluetoothSocket is not connected")
            }
        } catch (e: Exception) {
            Log.e("BluetoothHandler", "Error sending data", e)
        }
    }

    fun startReceiving(onDataReceived: (String) -> Unit) {
        if (bluetoothSocket?.isConnected == true) {
            inputStream = bluetoothSocket!!.inputStream

            Thread {
                val buffer = ByteArray(1024)
                var bytes: Int

                while (true) {
                    try {
                        bytes = inputStream.read(buffer)
                        val receivedData = String(buffer, 0, bytes).trim()
                        onDataReceived(receivedData)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        break
                    }
                }
            }.start()
        }
    }
}