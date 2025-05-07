package com.zanila.ladygaga.network

import com.zanila.ladygaga.api.PuntoGrafica
import retrofit2.http.GET

interface TemperatureChartApi {
    @GET("temperature_chart_data")
    suspend fun getTemperatureChartData(): Map<String, List<PuntoGrafica>>
}