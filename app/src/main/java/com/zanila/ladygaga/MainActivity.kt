package com.zanila.ladygaga

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.zanila.ladygaga.api.PuntoGrafica
import com.zanila.ladygaga.network.PythonApiClient
import com.zanila.ladygaga.viewmodel.SensorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    lateinit var temperaturaText: TextView
    lateinit var humedadText: TextView
    lateinit var luzText: TextView
    private lateinit var bluetoothHandler: BluetoothHandler
    private lateinit var charts: List<LineChart>
    private lateinit var dataSets: List<ILineDataSet>
    private var x = 0f
    private val handler = Handler(Looper.getMainLooper())

    private var currentChartIndex = 0
    internal var currentChartData: Map<String, List<PuntoGrafica>>? = null

    private val chartTitles = listOf(
        "Gráfica de Temperatura (Sensor)",
        "Gráfica de Humedad (Sensor)",
        "Gráfica de Luz (Sensor)",
        "Gráfica de Temperatura (Lógica Difusa)",
        "Gráfica 5 (Descripción)"
    )

    private val updateRunnable = object : Runnable {
        override fun run() {
            ChartUtils.updateChartData(x, charts, dataSets)
            x += 0.1f
            handler.postDelayed(this, 16)
        }
    }
    private val bluetoothDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_DATA_AVAILABLE -> {
                    val data = intent.getStringExtra(EXTRA_DATA)
                    data?.let {
                        procesarDatosBluetooth(it)
                    }
                }
            }
        }
    }

    private val viewModel: SensorViewModel by viewModels()

    private var temperaturaEntries = mutableListOf<Entry>()
    private var humedadEntries = mutableListOf<Entry>()
    private var luzEntries = mutableListOf<Entry>()
    private var maxDataPoints = 50 // Mantener solo los últimos 50 puntos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothHandler = BluetoothHandler(this)
        bluetoothHandler.checkAndRequestPermissions(this)

        // Inicializa las gráficas ANTES de crear el markerView
        charts = listOf(
            findViewById(R.id.lineChart1),
            findViewById(R.id.lineChart2),
            findViewById(R.id.lineChart3),
            findViewById(R.id.lineChart4),
            findViewById(R.id.lineChart5)
        )

        val markerView = CurrentTemperatureMarkerView(this, R.layout.custom_marker_view)
        charts[3].marker = markerView

        temperaturaText = findViewById(R.id.linea1)
        humedadText = findViewById(R.id.linea2)
        luzText = findViewById(R.id.linea3)

        bluetoothHandler.setDataReceivedListener { message ->
            procesarDatosBluetooth(message)
        }

        val buttonConnect = findViewById<Button>(R.id.btnConnect)
        buttonConnect.setOnClickListener {
            bluetoothHandler.connectToHC05(
                onConnected = { Toast.makeText(this, "Conectado al HC-05", Toast.LENGTH_SHORT).show() },
                onError = { errorMessage -> Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show() }
            )
        }

        val titleText = findViewById<TextView>(R.id.titleText)
        titleText.text = "Héctor Patricio"

        val led2 = findViewById<View>(R.id.led2)
        led2.setBackgroundResource(R.drawable.led_on)

        val light = findViewById<View>(R.id.lightIndicator)
        val intensity = 0.0f
        light.alpha = intensity

        updateTitleText()
        fetchTemperatureChartData()

        val chartColors = listOf(Color.MAGENTA, Color.BLUE, Color.GREEN, Color.RED, Color.YELLOW)
        dataSets = charts.mapIndexed { index, chart ->
            ChartUtils.setupChart(chart, chartColors[index])
        }

        ChartUtils.showChart(charts, currentChartIndex)

        val btnNext: View = findViewById(R.id.btnNext)
        val btnPrev: View = findViewById(R.id.btnPrev)

        btnNext.setOnClickListener {
            showNextChart()
        }

        btnPrev.setOnClickListener {
            showPreviousChart()
        }

        charts[0].setOnTouchListener(ChartUtils.setupTouchListenerForChartSwitching(charts[0]) { showNextChart() })

        handler.post(updateRunnable)

        // Observar los datos de los sensores
        viewModel.datosSensores.observe(this) { result ->
            result.onSuccess { datos ->
                datos["temperatura"]?.let { temperaturaText.text = "Temperatura: $it°C" }
                datos["humedad"]?.let { humedadText.text = "Humedad: $it%" }
                datos["luz"]?.let { luzText.text = "Luz: $it lux" }
            }.onFailure { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Observar los datos de la gráfica
        viewModel.datosGrafica.observe(this) { result ->
            result.onSuccess { datos ->
                updateTemperatureChart(datos)
            }.onFailure { error ->
                Toast.makeText(this, "Error al cargar gráfica: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Cargar datos iniciales
        viewModel.obtenerDatosGrafica()
    }

    private fun fetchTemperatureChartData() {
        val infoTextView = findViewById<TextView>(R.id.infoText)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chartData = PythonApiClient.temperatureChartApi.getTemperatureChartData()
                withContext(Dispatchers.Main) {
                    infoTextView?.text = "Conexión a la API exitosa. Datos recibidos."
                    updateTemperatureChart(chartData)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    infoTextView?.text = "Error al conectar con la API: ${e.message}"
                    Log.e(TAG, "Error fetching temperature chart data: ${e.message}")
                    Toast.makeText(this@MainActivity, "Error al cargar datos de la API", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    class XAxisTemperatureFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return if (value == 50f) {
                "Temperatura (°C)"
            } else {
                ""
            }
        }
    }

    class YAxisMembershipFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return if (value == 1f) {
                "Grado de Pertenencia"
            } else {
                ""
            }
        }
    }

    private fun updateTemperatureChart(chartData: Map<String, List<PuntoGrafica>>) {
        currentChartData = chartData
        val lineDataSets = mutableListOf<ILineDataSet>()

        for ((termName, dataPoints) in chartData) {
            val entries = dataPoints.map { dataPoint -> 
                Entry(dataPoint.value, dataPoint.membership, dataPoint)
            }
            val dataSet = LineDataSet(entries, termName).apply {
                when (termName) {
                    "frio" -> color = Color.BLUE
                    "templado" -> color = Color.BLACK
                    "caliente" -> color = Color.RED
                    else -> color = Color.GRAY
                }
                setDrawCircles(false)
            }
            lineDataSets.add(dataSet)
        }

        val lineData = LineData(lineDataSets)
        charts[3].data = lineData
        charts[3].invalidate()
    }

    private fun procesarDatosBluetooth(data: String) {
        try {
            Log.d(TAG, "Datos recibidos del Bluetooth: $data")
            // Asumiendo que los datos vienen en formato "temperatura,humedad,luz"
            val valores = data.split(",").map { it.toFloat() }
            Log.d(TAG, "Valores procesados: $valores")
            
            if (valores.size == 3) {
                val (temperatura, humedad, luz) = valores
                
                // Actualizar UI localmente
                runOnUiThread {
                    temperaturaText.text = "Temperatura: $temperatura°C"
                    humedadText.text = "Humedad: $humedad%"
                    luzText.text = "Luz: $luz lux"
                    Log.d(TAG, "TextViews actualizados: T=$temperatura, H=$humedad, L=$luz")
                }

                // Agregar nuevos puntos a las listas
                temperaturaEntries.add(Entry(x, temperatura))
                humedadEntries.add(Entry(x, humedad))
                luzEntries.add(Entry(x, luz))

                // Mantener solo los últimos maxDataPoints puntos
                if (temperaturaEntries.size > maxDataPoints) {
                    temperaturaEntries.removeAt(0)
                    humedadEntries.removeAt(0)
                    luzEntries.removeAt(0)
                }

                // Crear los datasets
                val dataSet1 = LineDataSet(temperaturaEntries, "Temperatura").apply {
                    color = Color.RED
                    setDrawCircles(false)
                    lineWidth = 2f
                }
                val dataSet2 = LineDataSet(humedadEntries, "Humedad").apply {
                    color = Color.BLUE
                    setDrawCircles(false)
                    lineWidth = 2f
                }
                val dataSet3 = LineDataSet(luzEntries, "Luz").apply {
                    color = Color.GREEN
                    setDrawCircles(false)
                    lineWidth = 2f
                }

                // Actualizar las gráficas
                runOnUiThread {
                    charts[0].data = LineData(dataSet1)
                    charts[1].data = LineData(dataSet2)
                    charts[2].data = LineData(dataSet3)

                    // Configurar los ejes
                    charts[0].xAxis.setDrawGridLines(false)
                    charts[0].axisLeft.setDrawGridLines(true)
                    charts[0].axisRight.isEnabled = false
                    charts[0].description.isEnabled = false
                    charts[0].legend.isEnabled = true

                    charts[1].xAxis.setDrawGridLines(false)
                    charts[1].axisLeft.setDrawGridLines(true)
                    charts[1].axisRight.isEnabled = false
                    charts[1].description.isEnabled = false
                    charts[1].legend.isEnabled = true

                    charts[2].xAxis.setDrawGridLines(false)
                    charts[2].axisLeft.setDrawGridLines(true)
                    charts[2].axisRight.isEnabled = false
                    charts[2].description.isEnabled = false
                    charts[2].legend.isEnabled = true

                    // Invalidar las gráficas para que se redibujen
                    charts[0].invalidate()
                    charts[1].invalidate()
                    charts[2].invalidate()
                    Log.d(TAG, "Gráficas actualizadas")
                }

                // Incrementar x para el siguiente punto
                x += 1f

                // Enviar datos al servidor
                viewModel.enviarDatosSensores(temperatura, humedad, luz)
            } else {
                Log.e(TAG, "Formato de datos incorrecto. Se esperaban 3 valores, se recibieron ${valores.size}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando datos Bluetooth: ${e.message}", e)
        }
    }

    private fun showNextChart() {
        currentChartIndex = (currentChartIndex + 1) % charts.size
        ChartUtils.showChart(charts, currentChartIndex)
    }

    private fun showPreviousChart() {
        currentChartIndex = (currentChartIndex - 1 + charts.size) % charts.size
        ChartUtils.showChart(charts, currentChartIndex)
    }

    private fun updateTitleText() {
        val titleText = findViewById<TextView>(R.id.titleText)
        titleText.text = chartTitles[currentChartIndex]
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothHandler.disconnect()
        handler.removeCallbacks(updateRunnable)
        unregisterReceiver(bluetoothDataReceiver)
    }

    companion object {
        private const val TAG = "MainActivity"
        const val ACTION_DATA_AVAILABLE = "com.zanila.ladygaga.ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "com.zanila.ladygaga.EXTRA_DATA"
    }
}