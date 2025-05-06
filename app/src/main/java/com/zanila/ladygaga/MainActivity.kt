package com.zanila.ladygaga

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.LineDataSet

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothHandler: BluetoothHandler
    private lateinit var charts: List<LineChart>
    private lateinit var dataSets: List<LineDataSet>
    private var x = 0f
    private val handler = Handler(Looper.getMainLooper())
    private var currentChartIndex = 0

    private val updateRunnable = object : Runnable {
        override fun run() {
            ChartUtils.updateChartData(x, charts, dataSets)
            x += 0.1f
            handler.postDelayed(this, 16)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothHandler = BluetoothHandler(this)
        bluetoothHandler.checkAndRequestPermissions(this)

        val buttonConnect = findViewById<Button>(R.id.btnConnect)
        buttonConnect.setOnClickListener {
            bluetoothHandler.connectToHC05(
                onConnected = { Toast.makeText(this, "Conectado al HC-05", Toast.LENGTH_SHORT).show() },
                onError = { errorMessage -> Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show() }
            )
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

        charts = listOf(
            findViewById(R.id.lineChart1),
            findViewById(R.id.lineChart2),
            findViewById(R.id.lineChart3),
            findViewById(R.id.lineChart4),
            findViewById(R.id.lineChart5)
        )

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

        // Establecer el OnTouchListener para el primer gráfico
        charts[0].setOnTouchListener(ChartUtils.setupTouchListenerForChartSwitching(charts[0]) { showNextChart() })

        handler.post(updateRunnable)
    }

    private fun showPreviousChart() {
        ChartUtils.showChart(charts, currentChartIndex)
        charts[currentChartIndex].setOnTouchListener(null) // Remove previous listener

        currentChartIndex = if (currentChartIndex - 1 < 0) charts.size - 1 else currentChartIndex - 1

        ChartUtils.showChart(charts, currentChartIndex)
        charts[currentChartIndex].setOnTouchListener(ChartUtils.setupTouchListenerForChartSwitching(charts[currentChartIndex]) { showNextChart() })
    }

    private fun showNextChart() {
        ChartUtils.showChart(charts, currentChartIndex)
        charts[currentChartIndex].setOnTouchListener(null) // Remove previous listener

        currentChartIndex = (currentChartIndex + 1) % charts.size

        ChartUtils.showChart(charts, currentChartIndex)
        charts[currentChartIndex].setOnTouchListener(ChartUtils.setupTouchListenerForChartSwitching(charts[currentChartIndex]) { showNextChart() })
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothHandler.disconnect()
        handler.removeCallbacks(updateRunnable) // Importante detener el Handler
    }
}