package ru.mstrike.msshell2

import android.app.Application

class Application: Application() {

    lateinit var optionsStorage: OptionsStorage

    override fun onCreate() {
        super.onCreate()
    }
}