package com.example.myapplication

import android.app.Application
import zerobranch.androidremotedebugger.AndroidRemoteDebugger

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        AndroidRemoteDebugger.init(
            AndroidRemoteDebugger.Builder(applicationContext).apply {
                // turn the debugger on/off
                enabled(true)

                // optional builder tweaks – call only the ones you really need
                disableInternalLogging()
                enableDuplicateLogging()
                disableJsonPrettyPrint()
                disableNotifications()
                excludeUncaughtException()

                // custom port (defaults to 9393 if you omit this)
                port(8080)
            }.build()
        )

    }
}