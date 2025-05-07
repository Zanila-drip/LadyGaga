package com.zanila.ladygaga

import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlin.math.sin

object ChartUtils {

    fun setupChart(chart: LineChart, color: Int): ILineDataSet {
        val dataSet = LineDataSet(null, "Tiempo real").apply {
            this.color = color
            setDrawCircles(false)
            lineWidth = 2f
            mode = LineDataSet.Mode.LINEAR
        }

        chart.apply {
            data = LineData(dataSet)
            setTouchEnabled(true)
            setScaleEnabled(false)
            description.isEnabled = false
            legend.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
            }

            axisRight.isEnabled = false
            axisLeft.axisMinimum = -1.5f
            axisLeft.axisMaximum = 1.5f
        }
        return dataSet
    }

    fun updateChartData(x: Float, charts: List<LineChart>, dataSets: List<ILineDataSet>) {
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
            val dataSet = dataSets[index] as LineDataSet
            dataSet.addEntry(Entry(x, yValues[index]))

            if (dataSet.entryCount > 100) {
                dataSet.removeFirst()
            }

            chart.data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.moveViewToX(x)
        }
    }

    fun showChart(charts: List<LineChart>, index: Int) {
        charts.forEachIndexed { i, chart ->
            chart.visibility = if (i == index) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    fun setupTouchListenerForChartSwitching(chart: LineChart, onNext: () -> Unit): View.OnTouchListener {
        return View.OnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                onNext()
                true
            } else {
                false
            }
        }
    }
}