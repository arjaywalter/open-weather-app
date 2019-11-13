package com.arjaywalter.openweatherapp.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arjaywalter.openweatherapp.model.WeatherItem
import com.arjaywalter.openweatherapp.repository.WeatherRepository

class MainViewModel(val repo: WeatherRepository) : ViewModel() {

    private lateinit var weather: MutableLiveData<WeatherItem>

    fun getWeather(): LiveData<WeatherItem> {
        if (!::weather.isInitialized) {
            weather = MutableLiveData()
        }
        return weather
    }

    fun loadWeather(latitude: Double, longitude: Double): LiveData<WeatherItem> {
        weather = repo.fetchWeather(latitude, longitude)
        return weather
    }

}
