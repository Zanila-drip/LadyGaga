package com.zanila.ladygaga

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private var bluetoothSocket: BluetoothSocket? = null
    private var isBluetoothConnected = false
    private val TAG = "MainActivity"
    private val REQUEST_ENABLE_BT = 1
    private val PERMISSION_REQUEST_CODE = 1
    private var isLedOn = false
    private var isProcessing = false
    
    // URL de la API (siempre producción)
    private val API_URL = "https://just-integrity-production-cc6a.up.railway.app/api/sensors"

    // UI Elements
    private lateinit var temperaturaText: TextView
    private lateinit var humedadText: TextView
    private lateinit var luzText: TextView
    private lateinit var humedadSueloText: TextView
    private lateinit var temperaturaTextAnterior: TextView
    private lateinit var humedadTextAnterior: TextView
    private lateinit var luzTextAnterior: TextView
    private lateinit var humedadSueloTextAnterior: TextView
    private lateinit var btnConnect: Button
    private lateinit var btnSmile: Button
    private lateinit var btnSendData: Button
    private lateinit var btnSendSimulatedData: Button
    private lateinit var processingStatus: TextView
    private lateinit var pumpRecommendation: TextView
    private lateinit var bluetoothHandler: BluetoothHandler
    private lateinit var switchSimulacion: Switch

    // Variables para almacenar valores anteriores
    private var ultimaActualizacion: Long = 0
    private var temperaturaAnterior: Float = 0f
    private var humedadAnterior: Float = 0f
    private var luzAnterior: Float = 0f
    private var humedadSueloAnterior: Float = 0f

    // Variables para simulación
    private val random = Random.Default

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

        // Iniciar verificación periódica de la bomba
        startPumpCheck()
    }

    private fun initializeUI() {
        temperaturaText = findViewById(R.id.linea1)
        humedadText = findViewById(R.id.linea2)
        luzText = findViewById(R.id.linea3)
        humedadSueloText = findViewById(R.id.infoText)
        temperaturaTextAnterior = findViewById(R.id.linea1_anterior)
        humedadTextAnterior = findViewById(R.id.linea2_anterior)
        luzTextAnterior = findViewById(R.id.linea3_anterior)
        humedadSueloTextAnterior = findViewById(R.id.infoText_anterior)
        btnConnect = findViewById(R.id.btnConnect)
        btnSmile = findViewById(R.id.btnSmile)
        btnSendData = findViewById(R.id.btnSendData)
        btnSendSimulatedData = findViewById(R.id.btnSendSimulatedData)
        processingStatus = findViewById(R.id.processingStatus)
        pumpRecommendation = findViewById(R.id.pumpRecommendation)
        switchSimulacion = findViewById(R.id.switchSimulacion)

        val titleText = findViewById<TextView>(R.id.titleText)
        titleText.text = "Monitor de Sensores"
    }

    private fun startPumpCheck() {
        // Verificar la bomba cada 30 segundos
        Thread {
            while (true) {
                if (isBluetoothConnected) {
                    checkPumpStatus()
                }
                Thread.sleep(30000) // 30 segundos
            }
        }.start()
    }

    private fun checkPumpStatus() {
        if (!isNetworkAvailable()) {
            Log.e(TAG, "No hay conexión a internet")
            runOnUiThread {
                Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            }
            return
        }

        try {
            val url = URL("$API_URL/pump")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                
                runOnUiThread {
                    if (jsonResponse.getBoolean("activar")) {
                        // Obtener tiempo de activación
                        val tiempoSegundos = jsonResponse.getInt("tiempo_segundos")
                        
                        // Mostrar recomendación
                        val razones = jsonResponse.getJSONArray("razones")
                        val razonesList = mutableListOf<String>()
                        for (i in 0 until razones.length()) {
                            razonesList.add(razones.getString(i))
                        }
                        
                        val recomendacion = "Recomendación: Activar bomba de agua\n" +
                                          "Tiempo: $tiempoSegundos segundos\n" +
                                          "Razones:\n" +
                                          razonesList.joinToString("\n") { "• $it" }
                        
                        pumpRecommendation.text = recomendacion
                        pumpRecommendation.visibility = View.VISIBLE
                        
                        // Cambiar el botón de la bomba
                        btnSmile.text = "💧"
                        btnSmile.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_light))
                        
                        // Activar la bomba automáticamente
                        if (isBluetoothConnected) {
                            isLedOn = true
                            bluetoothHandler.sendData("0") // Activar bomba
                            
                            // Programar desactivación después del tiempo especificado
                            Handler(Looper.getMainLooper()).postDelayed({
                                if (isBluetoothConnected) {
                                    isLedOn = false
                                    bluetoothHandler.sendData("1") // Desactivar bomba
                                    btnSmile.text = ":)"
                                    btnSmile.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
                                }
                            }, tiempoSegundos * 1000L) // Convertir segundos a milisegundos
                        }
                    } else {
                        // Ocultar recomendación
                        pumpRecommendation.visibility = View.GONE
                        
                        // Restaurar el botón de la bomba
                        btnSmile.text = ":)"
                        btnSmile.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar estado de la bomba", e)
            runOnUiThread {
                Toast.makeText(this, "Error al conectar con el servidor", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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
                    btnSendData.isEnabled = true
                    btnSendSimulatedData.isEnabled = true
                },
                onError = { errorMessage -> 
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    isBluetoothConnected = false
                    btnSmile.isEnabled = false
                    btnSendData.isEnabled = false
                    btnSendSimulatedData.isEnabled = false
                }
            )
        }

        switchSimulacion.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Generar datos simulados inmediatamente
                simularDatos()
                // Desactivar el switch
                switchSimulacion.isChecked = false
                Toast.makeText(this, "Datos simulados generados", Toast.LENGTH_SHORT).show()
            }
        }

        btnSendData.setOnClickListener {
            if (isBluetoothConnected && !isProcessing) {
                if (!isNetworkAvailable()) {
                    Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                isProcessing = true
                processingStatus.visibility = View.VISIBLE
                
                // Crear objeto con los datos reales de los sensores
                val sensorData = mapOf(
                    "temperatura" to temperaturaText.text.toString().replace("Temperatura: ", "").replace("°C", ""),
                    "humedad" to humedadText.text.toString().replace("Humedad: ", "").replace("%", ""),
                    "luz" to luzText.text.toString().replace("Luz: ", "").replace(" lux", ""),
                    "humedadSuelo" to humedadSueloText.text.toString().replace("Humedad Suelo: ", "").replace("%", "")
                )

                enviarDatosAlBackend(sensorData, "reales")
            } else if (!isBluetoothConnected) {
                Toast.makeText(this, "Primero debes conectar el dispositivo", Toast.LENGTH_SHORT).show()
            }
        }

        btnSendSimulatedData.setOnClickListener {
            if (isBluetoothConnected && !isProcessing) {
                if (!isNetworkAvailable()) {
                    Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                isProcessing = true
                processingStatus.visibility = View.VISIBLE
                
                // Crear objeto con los datos simulados
                val sensorData = mapOf(
                    "temperatura" to temperaturaTextAnterior.text.toString().replace("Temperatura: ", "").replace("°C", ""),
                    "humedad" to humedadTextAnterior.text.toString().replace("Humedad: ", "").replace("%", ""),
                    "luz" to luzTextAnterior.text.toString().replace("Luz: ", "").replace(" lux", ""),
                    "humedadSuelo" to humedadSueloTextAnterior.text.toString().replace("Humedad Suelo: ", "").replace("%", "")
                )

                enviarDatosAlBackend(sensorData, "simulados")
                
                // Desactivar el switch
                switchSimulacion.isChecked = false
            } else if (!isBluetoothConnected) {
                Toast.makeText(this, "Primero debes conectar el dispositivo", Toast.LENGTH_SHORT).show()
            }
        }

        btnSmile.setOnClickListener {
            if (isBluetoothConnected) {
                isLedOn = !isLedOn
                val command = if (isLedOn) "0" else "1"
                bluetoothHandler.sendData(command)
                btnSmile.text = if (isLedOn) "💧" else ":)"
                Toast.makeText(this, if (isLedOn) "Bomba activada" else "Bomba desactivada", Toast.LENGTH_SHORT).show()
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

                // Actualizar valores anteriores si han pasado 30 minutos
                val tiempoActual = System.currentTimeMillis()
                if (tiempoActual - ultimaActualizacion > 30 * 60 * 1000) { // 30 minutos en milisegundos
                    temperaturaAnterior = temperatura
                    humedadAnterior = humedad
                    luzAnterior = luz
                    humedadSueloAnterior = humedadSuelo
                    ultimaActualizacion = tiempoActual

                    // Actualizar UI con valores anteriores
                    temperaturaTextAnterior.text = "Temperatura: $temperaturaAnterior°C"
                    humedadTextAnterior.text = "Humedad: $humedadAnterior%"
                    luzTextAnterior.text = "Luz: $luzAnterior lux"
                    humedadSueloTextAnterior.text = "Humedad Suelo: $humedadSueloAnterior%"
                }

                // Actualizar UI con valores actuales
                temperaturaText.text = "Temperatura: $temperatura°C"
                humedadText.text = "Humedad: $humedad%"
                luzText.text = "Luz: $luz lux"
                humedadSueloText.text = "Humedad Suelo: $humedadSuelo%"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando datos: ${e.message}")
        }
    }

    private fun enviarDatosAlBackend(sensorData: Map<String, String>, tipo: String) {
        Thread {
            try {
                val url = URL(API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val jsonData = JSONObject(sensorData).toString()
                Log.d(TAG, "Enviando datos $tipo: $jsonData")
                
                val outputStream = connection.outputStream
                outputStream.write(jsonData.toByteArray())
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Respuesta del servidor ($tipo): $response")

                runOnUiThread {
                    isProcessing = false
                    processingStatus.visibility = View.GONE
                    if (responseCode == 200) {
                        try {
                            val jsonResponse = JSONObject(response)
                            if (jsonResponse.has("evaluacion")) {
                                val evaluacion = jsonResponse.getJSONObject("evaluacion")
                                val estado = evaluacion.getDouble("estado")
                                val recomendaciones = evaluacion.getJSONArray("recomendaciones")
                                val condiciones = evaluacion.getJSONObject("condiciones")
                                
                                val mensaje = """
                                    Datos $tipo enviados
                                    
                                    Estado de la planta: ${String.format("%.1f", estado)}%
                                    
                                    Recomendaciones:
                                    ${(0 until recomendaciones.length()).joinToString("\n") { "• ${recomendaciones.getString(it)}" }}
                                    
                                    Condiciones actuales:
                                    • Temperatura: ${condiciones.getDouble("temperatura")}°C
                                    • Humedad: ${condiciones.getDouble("humedad")}%
                                    • Suelo: ${condiciones.getDouble("suelo")}
                                    • Luz: ${condiciones.getDouble("luz")} lux
                                """.trimIndent()
                                
                                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this, "Datos $tipo enviados", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error procesando respuesta", e)
                            Toast.makeText(this, "Datos $tipo enviados", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Error al enviar datos $tipo", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando datos $tipo", e)
                runOnUiThread {
                    isProcessing = false
                    processingStatus.visibility = View.GONE
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun simularDatos() {
        // Simular diferentes escenarios
        val escenario = random.nextInt(4) // 0-3 para diferentes escenarios

        when (escenario) {
            0 -> { // Escenario: Planta seca
                simularValores(
                    temperatura = 35f, // Alta temperatura
                    humedad = 30f,     // Baja humedad
                    luz = 900f,        // Mucha luz
                    suelo = 200f       // Suelo seco
                )
            }
            1 -> { // Escenario: Planta húmeda
                simularValores(
                    temperatura = 22f,  // Temperatura óptima
                    humedad = 80f,     // Alta humedad
                    luz = 600f,        // Luz óptima
                    suelo = 800f       // Suelo húmedo
                )
            }
            2 -> { // Escenario: Condiciones óptimas
                simularValores(
                    temperatura = 25f,  // Temperatura óptima
                    humedad = 60f,     // Humedad óptima
                    luz = 600f,        // Luz óptima
                    suelo = 600f       // Suelo óptimo
                )
            }
            3 -> { // Escenario: Planta fría
                simularValores(
                    temperatura = 15f,  // Baja temperatura
                    humedad = 70f,     // Humedad alta
                    luz = 300f,        // Poca luz
                    suelo = 700f       // Suelo húmedo
                )
            }
        }
    }

    private fun simularValores(temperatura: Float, humedad: Float, luz: Float, suelo: Float) {
        // Actualizar valores simulados
        temperaturaTextAnterior.text = "Temperatura: $temperatura°C"
        humedadTextAnterior.text = "Humedad: $humedad%"
        luzTextAnterior.text = "Luz: $luz lux"
        humedadSueloTextAnterior.text = "Humedad Suelo: $suelo%"
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothHandler.disconnect()
    }
}