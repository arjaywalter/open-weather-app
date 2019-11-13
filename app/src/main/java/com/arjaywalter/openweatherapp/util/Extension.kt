package com.arjaywalter.openweatherapp.util

fun String.toIconUrl(): String {
    return "https://openweathermap.org/img/wn/$this@2x.png"
}
