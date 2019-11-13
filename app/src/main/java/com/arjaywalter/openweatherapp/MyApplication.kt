package com.arjaywalter.openweatherapp

import android.app.Application
import com.arjaywalter.openweatherapp.api.WeatherService
import com.arjaywalter.openweatherapp.repository.WeatherRepository
import com.arjaywalter.openweatherapp.ui.main.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MyApplication: Application(){
    override fun onCreate() {
        super.onCreate()
        // Start Koin
        startKoin{
            androidLogger()
            androidContext(this@MyApplication)
            modules(appModule)
        }
    }


    val appModule = module {

        // single instance of WeatherService
        single { WeatherService.create() }

        // single instance of WeatherRepository
        single { WeatherRepository(get()) }

        // MainViewModel ViewModel
        viewModel { MainViewModel(get()) }
    }
}
