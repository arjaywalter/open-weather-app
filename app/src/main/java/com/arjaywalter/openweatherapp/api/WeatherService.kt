/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arjaywalter.openweatherapp.api

import android.util.Log
import com.arjaywalter.openweatherapp.model.WeatherItem
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val TAG = "WeatherService"
private const val IN_QUALIFIER = "in:name,description"


fun getWeatherByCoordinates(
    lat: Double,
    lon: Double,
    service: WeatherService,
    onSuccess: (item: WeatherItem) -> Unit,
    onError: (error: String) -> Unit
) {

    service.getWeatherByCoordinates(lat, lon).enqueue(
        object : Callback<WeatherItem> {
            override fun onFailure(call: Call<WeatherItem>?, t: Throwable) {
                Log.d(TAG, "fail to get data")
                onError(t.message ?: "unknown error")
            }

            override fun onResponse(
                call: Call<WeatherItem>?,
                response: Response<WeatherItem>
            ) {
                Log.d(TAG, "got a response $response")
                if (response.isSuccessful) {
                    val item = response.body()
                    item?.let { onSuccess(it) }
                } else {
                    onError(response.errorBody()?.string() ?: "Unknown error")
                }
            }
        }
    )
}


/**
 * Github API communication setup via Retrofit.
 */
interface WeatherService {

    @GET("weather")
    fun getWeatherByCoordinates(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String? = API_KEY
    ): Call<WeatherItem>


    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
        const val API_KEY = "c2b653562b0916bc48f9496aee691cc1"

        fun create(): WeatherService {
            val logger = HttpLoggingInterceptor()
            logger.level = Level.BASIC

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WeatherService::class.java)
        }
    }
}