package com.zanila.ladygaga.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SensorViewModel : ViewModel() {
    private val _datosSensores = MutableLiveData<Result<Map<String, Float>>>()
    val datosSensores: LiveData<Result<Map<String, Float>>> = _datosSensores

    fun actualizarDatosSensores(temperatura: Float, humedad: Float, luz: Float) {
        _datosSensores.value = Result.success(
            mapOf(
                "temperatura" to temperatura,
                "humedad" to humedad,
                "luz" to luz
            )
        )
    }
} 