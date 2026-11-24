package com.keshav.capturesposed

import android.os.Build
import android.util.Log
import com.keshav.capturesposed.hookers.ScreenRecordingCallbackControllerHooker
import com.keshav.capturesposed.hookers.WindowManagerServiceHooker
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam

private lateinit var module: CaptureSposed

const val TAG = "CaptureSposed"

class CaptureSposed : XposedModule() {
    override fun onModuleLoaded(param: ModuleLoadedParam) {
        super.onModuleLoaded(param)
        module = this
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        super.onSystemServerStarting(param)

        try {
            WindowManagerServiceHooker.hook(param.classLoader, module)
        } catch (e: Exception) {
            log(Log.ERROR, TAG, "WindowManagerServiceHooker failed: $e")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            try {
                ScreenRecordingCallbackControllerHooker.hook(param.classLoader, module)
            } catch (e: Exception) {
                log(Log.ERROR, TAG, "ScreenRecordingCallbackControllerHooker failed: $e")
            }
        }
    }

    override fun onHotReloading(param: HotReloadingParam): Boolean {
        WindowManagerServiceHooker.clearGenerationRefs()
        ScreenRecordingCallbackControllerHooker.clearGenerationRefs()
        return true
    }

    override fun onHotReloaded(param: HotReloadedParam) {
        /*
            onSystemServerStarting is not replayed after hot reload. The default implementation
            would unhook every old handle before we can replaceHook them with the new generation.
         */
        module = this

        if (!param.isSystemServer) {
            log(Log.WARN, TAG, "Hot reload skipped: not system_server")
            return
        }

        try {
            val handles = param.oldHookHandles
            if (handles.isEmpty()) {
                log(Log.ERROR, TAG, "ERROR: hot reload delivered no hook handles to replace")
                return
            }

            val claimed = mutableSetOf<XposedInterface.HookHandle>()
            claimed.addAll(WindowManagerServiceHooker.replaceHooks(handles, this))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
                claimed.addAll(ScreenRecordingCallbackControllerHooker.replaceHooks(handles, this))

            for (handle in handles) {
                if (handle !in claimed) {
                    log(
                        Log.WARN,
                        TAG,
                        "Unhooking stale handle: ${handle.id ?: handle.executable.name}",
                    )
                    handle.unhook()
                }
            }

            log(Log.INFO, TAG, "Hot reload completed successfully")
        } catch (e: Exception) {
            log(Log.ERROR, TAG, "Hot reload failed: $e")
        }
    }
}
