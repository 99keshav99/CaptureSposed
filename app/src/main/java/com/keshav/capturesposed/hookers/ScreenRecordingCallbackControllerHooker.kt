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
    private var module: XposedModule? = null
    private var classLoader: ClassLoader? = null
    private var mediaProjectionInfoCache: Any? = null
    var allowMediaProjectionInfoCacheWipe = true

    @SuppressLint("PrivateApi")
    fun hook(param: SystemServerLoadedParam, module: XposedModule) {
        this.module = module
        classLoader = param.classLoader

        val screenRecordingCallbackControllerClass = param.classLoader.loadClass("com.android.server.wm.ScreenRecordingCallbackController")
        val onScreenRecordingStartMethod = screenRecordingCallbackControllerClass.getDeclaredMethod("onScreenRecordingStart",
            classLoader!!.loadClass("android.media.projection.MediaProjectionInfo"))
        val onScreenRecordingStopMethod = screenRecordingCallbackControllerClass.getDeclaredMethod("onScreenRecordingStop")

        module.hook(onScreenRecordingStartMethod, OnScreenRecordingStartHooker::class.java)
        module.hook(onScreenRecordingStopMethod, OnScreenRecordingStopHooker::class.java)
    }

    @XposedHooker
    private class OnScreenRecordingStartHooker: Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun beforeInvocation(callback: BeforeHookCallback) {
                val prefs = module?.getRemotePreferences(BuildConfig.APPLICATION_ID)
                val isHookActive = prefs?.getBoolean("screenRecordHookActive", true)

                /*
                    When the method is invoked via our injected window manager command, the argument is set to null.
                    This indicates that the mediaProjectionInfo object that we cached earlier should be used instead.
                    If the argument is not null, it means it should be cached in case it is needed for later use.
                 */
                if (callback.args[0] == null)
                    callback.args[0] = mediaProjectionInfoCache
                else
                    mediaProjectionInfoCache = callback.args[0]

                /*
                    If the argument is still null at this point, it means that there is no active recording in
                    progress so the function's real code should be skipped. If the hook is active, it means
                    that the function's real code should be skipped regardless of if a recording is in progress.
                 */
                if (callback.args[0] == null || isHookActive!!)
                    callback.returnAndSkip(null)
            }
        }
    }

    @XposedHooker
    private class OnScreenRecordingStopHooker: Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun beforeInvocation(callback: BeforeHookCallback) {
                /*
                    allowMediaProjectionInfoCacheWipe is toggled by the injected window manager command to prevent
                    the cache from being wiped when disabling the screen recording detection hook whenever a
                    recording is taking place.
                 */
                if (allowMediaProjectionInfoCacheWipe)
                    mediaProjectionInfoCache = null
            }
        }
    }
}