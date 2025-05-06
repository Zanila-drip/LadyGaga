package com.zanila.ladygaga

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.button.MaterialButton
import kotlin.math.sin
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private val REQUEST_BLUETOOTH_PERMISSIONS = 2
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val hc05MacAddress = "00:23:10:00:D3:38"


    private lateinit var charts: List<LineChart>
    private lateinit var dataSets: List<LineDataSet>
    private var x = 0f
    private val handler = Handler(Looper.getMainLooper())
    private var currentChartIndex = 0

    private val updateRunnable = object : Runnable {
        override fun run() {

            val yValues = (0 until charts.size).map { index ->
                when (index) {
                    0 -> sin(x.toDouble()).toFloat()
                    1 -> (sin(x.toDouble() * 2) * 0.5).toFloat()
                    2 -> (sin(x.toDouble() * 0.7) * 1.2).toFloat()
                    3 -> (sin(x.toDouble() * 3 + 1) * 0.8).toFloat()
                    4 -> (sin(x.toDouble() * 1.5 - 0.5) * 1.1).toFloat()
                    else -> 0f
                }
            }

            charts.forEachIndexed { index, chart ->
                val dataSet = dataSets[index]
                dataSet.addEntry(Entry(x, yValues[index]))

                if (dataSet.entryCount > 100) {
                    dataSet.removeFirst()
                }

                chart.data.notifyDataChanged()
                chart.notifyDataSetChanged()
                chart.moveViewToX(x)
            }

            x += 0.1f
            handler.postDelayed(this, 16) // ~60 FPS
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val buttonConnect = findViewById<Button>(R.id.btnConnect)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no disponible", Toast.LENGTH_LONG).show()
            finish()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                REQUEST_BLUETOOTH_PERMISSIONS
            )
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        fun connectToHC05() {
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            val device: BluetoothDevice? = pairedDevices?.find { it.address == hc05MacAddress }

            if (device == null) {
                Toast.makeText(this, "No se encontró el HC-05 emparejado", Toast.LENGTH_SHORT).show()
                return
            }

            val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID SPP
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)

            Thread @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN) {
                bluetoothAdapter?.cancelDiscovery()
                try {
                    bluetoothSocket?.connect()
                    outputStream = bluetoothSocket?.outputStream

                    runOnUiThread {
                        Toast.makeText(this, "Conectado al HC-05", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }
        buttonConnect.setOnClickListener {
            connectToHC05()
        }


        val titleText = findViewById<TextView>(R.id.titleText)
        titleText.text = "Héctor Patricio"
        val titleText2 = findViewById<TextView>(R.id.linea1)
        titleText2.text = "Héctor Patricio"

        val led2 = findViewById<View>(R.id.led2)
        led2.setBackgroundResource(R.drawable.led_on) // o .led_off

        val light = findViewById<View>(R.id.lightIndicator)
        val intensity = 0.0f // de 0.0 a 1.0
        light.alpha = intensity


//
        charts = listOf(
            findViewById(R.id.lineChart1),
            findViewById(R.id.lineChart2),
            findViewById(R.id.lineChart3),
            findViewById(R.id.lineChart4),
            findViewById(R.id.lineChart5)
        )

        dataSets = charts.map { setupChart(it) }
        charts.forEachIndexed { index, chart ->
            chart.visibility = if (index == 0) View.VISIBLE else View.GONE
        }


        val btnNext: View = findViewById(R.id.btnNext)
        val btnPrev: View = findViewById(R.id.btnPrev)

        btnNext.setOnClickListener {
            showNextChart()
        }

        btnPrev.setOnClickListener {
            showPreviousChart()
        }
        // Establecer el OnTouchListener para el primer gráfico
        charts[0].setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                showNextChart()
                true
            } else {
                false
            }
        }

        handler.post(updateRunnable)
    }

    private fun setupChart(chart: LineChart): LineDataSet {
        val color = when (charts.indexOf(chart)) {
            0 -> Color.MAGENTA
            1 -> Color.BLUE
            2 -> Color.GREEN
            3 -> Color.RED
            4 -> Color.YELLOW
            else -> Color.BLACK
        }

        val dataSet = LineDataSet(null, "Tiempo real").apply {
            this.color = color
            setDrawCircles(false)
            lineWidth = 2f
            mode = LineDataSet.Mode.LINEAR
        }

        chart.apply {
            data = LineData(dataSet)
            setTouchEnabled(true) // Habilitar toques en todos los gráficos (aunque solo uno sea visible)
            setScaleEnabled(false)
            description.isEnabled = false
            legend.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
            }

            axisRight.isEnabled = false
            axisLeft.axisMinimum = -1.5f // Ajustar rango para los diferentes senos
            axisLeft.axisMaximum = 1.5f
        }

        return dataSet
    }
    private fun showPreviousChart() {
        charts[currentChartIndex].visibility = View.GONE

        currentChartIndex = if (currentChartIndex - 1 < 0) charts.size - 1 else currentChartIndex - 1

        charts[currentChartIndex].visibility = View.VISIBLE
    }

    private fun showNextChart() {
        android.util.Log.d("MainActivity", "Índice actual (antes): $currentChartIndex")
        charts[currentChartIndex].visibility = View.GONE
        charts[currentChartIndex].setOnTouchListener(null)

        currentChartIndex = (currentChartIndex + 1) % charts.size
        android.util.Log.d("MainActivity", "Índice actual (después): $currentChartIndex")

        charts[currentChartIndex].visibility = View.VISIBLE
        charts[currentChartIndex].setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                showNextChart()
                true
            } else {
                false
            }
        }
        android.util.Log.d("MainActivity", "Mostrando gráfico con índice: $currentChartIndex")
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothSocket?.close()
    }
}