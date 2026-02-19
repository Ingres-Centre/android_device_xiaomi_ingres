package org.lineageos.leds

import android.app.Application
import android.os.ServiceManager
import vendor.lineage.leds.ILeds

class Application : Application() {
    val ledsService by lazy {
        val binder = ServiceManager.getServiceOrThrow(ILeds.DESCRIPTOR + "/default")
        ILeds.Stub.asInterface(binder)
    }

    companion object {
        private lateinit var _INSTANCE: org.lineageos.leds.Application
        val INSTANCE get() = _INSTANCE
    }

    override fun onCreate() {
        _INSTANCE = this

        super.onCreate()
    }
}