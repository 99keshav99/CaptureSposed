package com.keshav.capturesposed

import android.annotation.SuppressLint
import android.os.IBinder
import com.keshav.capturesposed.hookers.ActivityTaskManagerServiceHooker
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam

class CaptureSposed(base: XposedInterface, param: ModuleLoadedParam) : XposedModule(base, param) {

    @SuppressLint("PrivateApi")
    override fun onSystemServerLoaded(param: SystemServerLoadedParam) {
        super.onSystemServerLoaded(param)

        try {
            hook(
                Class.forName(
                    "com.android.server.wm.ActivityTaskManagerService", true, param.classLoader
                ).getDeclaredMethod(
                    "registerScreenCaptureObserver",
                    IBinder::class.java,
                    Class.forName("android.app.IScreenCaptureObserver")
                ), ActivityTaskManagerServiceHooker::class.java
            )
        } catch (e: Exception) {
            log("[CaptureSposed] ERROR: $e")
        }
    }
}