package com.zanila.ladygaga

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var bluetoothSocket: BluetoothSocket? = null
    private var isBluetoothConnected = false
    private val TAG = "MainActivity"
    private val REQUEST_ENABLE_BT = 1
    private val PERMISSION_REQUEST_CODE = 1
    private var isLedOn = false

    // UI Elements
    private lateinit var temperaturaText: TextView
    private lateinit var humedadText: TextView
    private lateinit var luzText: TextView
    private lateinit var humedadSueloText: TextView
    private lateinit var btnConnect: Button
    private lateinit var btnSmile: Button
    private lateinit var bluetoothHandler: BluetoothHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Verificar permisos y inicializar Bluetooth
        checkPermissions()

        bluetoothHandler = BluetoothHandler(this)
        bluetoothHandler.checkAndRequestPermissions(this)

        // Inicializar UI
        initializeUI()
        
        // Configurar listeners
        setupListeners()
    }

    private fun initializeUI() {
        temperaturaText = findViewById(R.id.linea1)
        humedadText = findViewById(R.id.linea2)
        luzText = findViewById(R.id.linea3)
        humedadSueloText = findViewById(R.id.infoText)
        btnConnect = findViewById(R.id.btnConnect)
        btnSmile = findViewById(R.id.btnSmile)

        val titleText = findViewById<TextView>(R.id.titleText)
        titleText.text = "Monitor de Sensores"
    }

    private fun setupListeners() {
        bluetoothHandler.setDataReceivedListener { message ->
            procesarDatosBluetooth(message)
        }

        btnConnect.setOnClickListener {
            bluetoothHandler.connectToHC05(
                onConnected = { 
                    Toast.makeText(this, "Conectado al HC-05", Toast.LENGTH_SHORT).show()
                    isBluetoothConnected = true
                    btnSmile.isEnabled = true
                },
                onError = { errorMessage -> 
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    isBluetoothConnected = false
                    btnSmile.isEnabled = false
                }
            )
        }

        btnSmile.setOnClickListener {
            if (isBluetoothConnected) {
                isLedOn = !isLedOn
                val command = if (isLedOn) "1" else "0"
                bluetoothHandler.sendData(command)
                btnSmile.text = if (isLedOn) ":)" else ":("
                Toast.makeText(this, if (isLedOn) "LED encendido" else "LED apagado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Primero debes conectar el dispositivo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissions() {
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
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permisos concedidos
            } else {
                Toast.makeText(
                    this,
                    "Se requieren permisos para usar Bluetooth",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun procesarDatosBluetooth(data: String) {
        try {
            // Asumimos que los datos vienen en formato "temperatura,humedad,luz,humedadSuelo"
            val valores = data.split(",")
            if (valores.size >= 4) {
                val temperatura = valores[0].toFloat()
                val humedad = valores[1].toFloat()
                val luz = valores[2].toFloat()
                val humedadSuelo = valores[3].toFloat()

                // Actualizar UI
                temperaturaText.text = "Temperatura: $temperatura°C"
                humedadText.text = "Humedad: $humedad%"
                luzText.text = "Luz: $luz lux"
                humedadSueloText.text = "Humedad Suelo: $humedadSuelo%"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando datos: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothHandler.disconnect()
    }
}