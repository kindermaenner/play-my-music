package de.kindermaenner.playmymusic

import android.app.Application
import android.util.Log

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // load the json mapping
        Log.d("MyApp", "Lade Mapping…")
        // MappingLoader.load(this)
    }
}