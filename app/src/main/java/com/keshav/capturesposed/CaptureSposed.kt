package com.keshav.capturesposed

import android.os.Build
import com.keshav.capturesposed.hookers.SystemUIHooker
import com.keshav.capturesposed.hookers.WindowManagerServiceHooker
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
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
        } catch (e: Exception) {
            log("[CaptureSposed] ERROR: $e")
        }
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        super.onPackageLoaded(param)

        when (param.packageName) {
            "com.android.systemui" -> {
                val prefs = getRemotePreferences(BuildConfig.APPLICATION_ID)
                if (!prefs.getBoolean("tileRevealDone", false)) {
                    try {
                        module.log("[CaptureSposed] Hooking System UI to add and reveal quick settings tile.")
                        SystemUIHooker.hook(param, module)
                    } catch (e: Exception) {
                        log("[CaptureSposed] ERROR: $e")
                    }
                }
            }
        }
    }
}