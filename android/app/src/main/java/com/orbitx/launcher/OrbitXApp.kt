package com.orbitx.launcher

import android.app.Application
import com.orbitx.launcher.core.OrbitLog
import com.orbitx.launcher.core.OrbitPaths
import com.orbitx.launcher.data.Store
import java.io.File

class OrbitXApp : Application() {
    override fun onCreate() {
        super.onCreate()
        OrbitPaths.init(filesDir)
        OrbitLog.attach(File(OrbitPaths.logsDir, "orbitx.log"))
        Store.init(this)
        OrbitLog.i("OrbitX Launcher ${BuildConfig.VERSION_NAME} started; abi=${com.orbitx.launcher.core.RuntimeCatalog.primaryAbi()}")
    }
}
