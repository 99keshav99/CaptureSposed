package com.keshav.capturesposed

import android.annotation.SuppressLint
import android.graphics.Paint.Cap
import android.os.IBinder
import com.keshav.capturesposed.hookers.ActivityTaskManagerServiceHooker
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam

private lateinit var module: CaptureSposed

class CaptureSposed(base: XposedInterface, param: ModuleLoadedParam) : XposedModule(base, param) {
    init {
        module = this
    }

    @SuppressLint("PrivateApi")
    override fun onSystemServerLoaded(param: SystemServerLoadedParam) {
        super.onSystemServerLoaded(param)

        try {
            ActivityTaskManagerServiceHooker.hook(param, module)
        } catch (e: Exception) {
            log("[CaptureSposed] ERROR: $e")
        }
    }
}