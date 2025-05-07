package com.zanila.ladygaga

import android.annotation.SuppressLint
import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.zanila.ladygaga.model.DataPoint

@SuppressLint("ViewConstructor")
class CurrentTemperatureMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    private val tvValue: TextView = findViewById(R.id.tvValue)
    private val tvMembership: TextView = findViewById(R.id.tvMembership)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e?.data is DataPoint) {
            val dataPoint = e.data as DataPoint
            tvValue.text = "Temperatura: ${dataPoint.value}°C"
            tvMembership.text = "Pertenencia: ${String.format("%.2f", dataPoint.membership)}"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}