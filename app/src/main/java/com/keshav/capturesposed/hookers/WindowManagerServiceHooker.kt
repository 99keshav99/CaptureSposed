package com.keshav.capturesposed.hookers

import android.annotation.SuppressLint
import android.content.ComponentName
import android.os.Build
import com.keshav.capturesposed.BuildConfig
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker

object WindowManagerServiceHooker {
    var module: XposedModule? = null
    var classLoader: ClassLoader? = null

    @SuppressLint("PrivateApi")
    fun hook(param: SystemServerLoadedParam, module: XposedModule) {
        this.module = module
        classLoader = param.classLoader
        val windowManagerServiceClass = param.classLoader.loadClass("com.android.server.wm.WindowManagerService")

        module.hook(
            windowManagerServiceClass.getDeclaredMethod("notifyScreenshotListeners", Int::class.java),
            NotifyScreenshotListenersHooker::class.java
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            module.hook(
                windowManagerServiceClass.getDeclaredMethod("registerScreenRecordingCallback",
                    classLoader!!.loadClass("android.window.IScreenRecordingCallback")),
                RegisterScreenRecordingCallbackHooker::class.java
            )
        }
    }

    @XposedHooker
    private class NotifyScreenshotListenersHooker: Hooker {
        companion object {
            @Suppress("unused")
            @JvmStatic
            @BeforeInvocation
            fun beforeInvocation(callback: BeforeHookCallback) {
                val prefs = module?.getRemotePreferences(BuildConfig.APPLICATION_ID)
                val isHookActive = prefs?.getBoolean("screenshotHookActive", true)

                if (isHookActive!!) {
                    module?.log("[CaptureSposed] Blocked screenshot detection.")
                    callback.returnAndSkip(listOf<ComponentName>())
                }
                else {
                    module?.log("[CaptureSposed] Allowed screenshot detection.")
                }
            }
        }
    }

    @XposedHooker
    private class RegisterScreenRecordingCallbackHooker: Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun beforeInvocation(callback: BeforeHookCallback) {
                val prefs = module?.getRemotePreferences(BuildConfig.APPLICATION_ID)
                val isHookActive = prefs?.getBoolean("screenRecordHookActive", true)

                if (isHookActive!!)
                    callback.returnAndSkip(false)
            }
        }
    }
}