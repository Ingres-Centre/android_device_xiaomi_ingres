package org.lineageos.gamekeys

import android.annotation.SuppressLint
import android.app.Application
import org.lineageos.gamekeys.model.AppViewModel

@SuppressLint("PrivateApi")
class Application : Application() {
    companion object {
        private lateinit var _INSTANCE: org.lineageos.gamekeys.Application
        val INSTANCE get() = _INSTANCE
    }

    val viewModel by lazy { AppViewModel(this) }

    override fun onCreate() {
        _INSTANCE = this

        super.onCreate()
    }
}
