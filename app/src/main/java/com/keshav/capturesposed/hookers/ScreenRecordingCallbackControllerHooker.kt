package com.keshav.capturesposed.hookers

import android.annotation.SuppressLint
import com.keshav.capturesposed.BuildConfig
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker

object ScreenRecordingCallbackControllerHooker {
    var module: XposedModule? = null
    var classLoader: ClassLoader? = null

    @SuppressLint("PrivateApi")
    fun hook(param: SystemServerLoadedParam, module: XposedModule) {
        this.module = module
        classLoader = param.classLoader

        module.hook(
            param.classLoader.loadClass("com.android.server.wm.ScreenRecordingCallbackController")
                .getDeclaredMethod("onScreenRecordingStart",
                    classLoader!!.loadClass("android.media.projection.MediaProjectionInfo")),
            OnScreenRecordingStartHooker::class.java
        )
    }

    @XposedHooker
    private object OnScreenRecordingStartHooker: Hooker {
        @JvmStatic
        @BeforeInvocation
        fun beforeInvocation(callback: BeforeHookCallback) {
            val prefs = WindowManagerServiceHooker.module?.getRemotePreferences(BuildConfig.APPLICATION_ID)
            val isHookActive = prefs?.getBoolean("screenRecordHookActive", true)

            if (isHookActive!!)
                callback.returnAndSkip(null)
        }
    }
}