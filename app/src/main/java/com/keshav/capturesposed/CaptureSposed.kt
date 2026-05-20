package com.keshav.capturesposed

import android.os.Build
import com.keshav.capturesposed.hookers.ScreenRecordingCallbackControllerHooker
import com.keshav.capturesposed.hookers.WindowManagerServiceHooker
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam

private lateinit var module: CaptureSposed

class CaptureSposed(base: XposedInterface, param: ModuleLoadedParam) : XposedModule(base, param) {
    init {
        module = this
    }

    override fun onSystemServerLoaded(param: SystemServerLoadedParam) {
        super.onSystemServerLoaded(param)

        try {
            WindowManagerServiceHooker.hook(param, module)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
                ScreenRecordingCallbackControllerHooker.hook(param, module)
        } catch (e: Exception) {
            log("[CaptureSposed] ERROR: $e")
        }
    }
}
