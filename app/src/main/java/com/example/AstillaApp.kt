package com.example

import android.app.Application
import android.util.Log
import com.example.data.local.AppDatabase

class AstillaApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Set default uncaught exception handler to prevent hard crash loops
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("AstillaApp", "Uncaught exception on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Initialize local database safely
        try {
            AppDatabase.getInstance(this)
        } catch (e: Exception) {
            Log.e("AstillaApp", "Failed to pre-initialize database", e)
        }
    }
}
