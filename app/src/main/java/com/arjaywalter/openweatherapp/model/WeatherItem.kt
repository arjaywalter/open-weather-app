package com.arjaywalter.openweatherapp.model

import androidx.room.*
import com.google.gson.annotations.SerializedName

@Entity(tableName = "weathers")
data class WeatherItem(
    @PrimaryKey val id: Int?,
    @Embedded var clouds: Clouds?,
    @Embedded @field:SerializedName("coord") val coord: Coord?,
    @field:SerializedName("dt") val dt: Int?,
    @Embedded @field:SerializedName("main") val main: Main?,
    @field:SerializedName("name") val name: String?,
//    @Embedded @field:SerializedName("sys") val sys: Sys?,
    @field:SerializedName("visibility") val visibility: Int?,
    @ColumnInfo(name = "weatherList")
    val weather: List<Weather>?
//    @Embedded@field:SerializedName("wind") val wind: Wind?
) {
    data class Main(
        val humidity: Int,
        val pressure: Int,
        val temp: Double,
        val temp_max: Double,
        val temp_min: Double
    )

    data class Wind(
        val deg: Int,
        val speed: Double
    )

    data class Weather(
        val description: String?,
        val icon: String?,
        //val id: Int,
        val main: String?
    )

    data class Sys(
        val country: String,
        //val id: Int,
        val message: Double,
        val sunrise: Int,
        val sunset: Int,
        val type: Int
    )

    data class Clouds(
        val all: Int?
    )

    data class Coord(
        val lat: Double,
        val lon: Double
    )
}