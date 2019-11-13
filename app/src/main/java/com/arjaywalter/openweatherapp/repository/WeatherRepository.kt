package com.arjaywalter.openweatherapp.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arjaywalter.openweatherapp.api.WeatherService
import com.arjaywalter.openweatherapp.api.getWeatherByCoordinates
import com.arjaywalter.openweatherapp.model.WeatherItem


/**
 * Repository class that works with local and remote data sources.
 */
class WeatherRepository(
    private val service: WeatherService
//    private val cache: WeatherLocalCache // TODO Room DB implementation
) {

    // LiveData of weather item
    private val data = MutableLiveData<WeatherItem>()

    // LiveData of network errors.
    private val networkErrors = MutableLiveData<String>()

    // avoid triggering multiple requests in the same time
    private var isRequestInProgress = false

    fun fetchWeather(lat: Double, lon: Double): MutableLiveData<WeatherItem> {

        if (isRequestInProgress) return data

        isRequestInProgress = true
        getWeatherByCoordinates(lat, lon, service, { item ->
            //            cache.insert(item) {
            data.postValue(item)
            isRequestInProgress = false
//            }
        }, { error ->
            //            data.postValue(cache.liveData(id).value)
            networkErrors.postValue(error)
            isRequestInProgress = false
        })

        return data
    }

}