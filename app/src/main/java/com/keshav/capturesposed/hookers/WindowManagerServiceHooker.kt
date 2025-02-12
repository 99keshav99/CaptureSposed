package com.keshav.capturesposed.hookers

import android.annotation.SuppressLint
import android.content.ComponentName
import android.os.Build
import android.os.ResultReceiver
import com.keshav.capturesposed.BuildConfig
import com.keshav.capturesposed.utils.XposedHelpers
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker
import java.io.FileDescriptor

object WindowManagerServiceHooker {
    var module: XposedModule? = null
    private var classLoader: ClassLoader? = null

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
                windowManagerServiceClass.getDeclaredMethod("onShellCommand", FileDescriptor::class.java,
                    FileDescriptor::class.java, FileDescriptor::class.java, Array<String>::class.java,
                    classLoader!!.loadClass("android.os.ShellCallback"), ResultReceiver::class.java),
                OnShellCommandHooker::class.java
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
    private class OnShellCommandHooker: Hooker {
        companion object {
            @SuppressLint("PrivateApi")
            @JvmStatic
            @BeforeInvocation
            fun beforeInvocation(callback: BeforeHookCallback) {
                // This will intercept command: wm refresh-recording-callbacks
                val wmCommandArgs = callback.args[3] as Array<*>
                if (wmCommandArgs.size == 1 && wmCommandArgs[0] == "refresh-recording-callbacks") {
                    val prefs = module?.getRemotePreferences(BuildConfig.APPLICATION_ID)
                    val isHookActive = prefs?.getBoolean("screenRecordHookActive", true)

                    val mScreenRecordingCallbackController = XposedHelpers.getObjectField(callback.thisObject,
                        "mScreenRecordingCallbackController") as Any
                    val screenRecordingCallbackControllerClass = mScreenRecordingCallbackController::class.java

                    if (isHookActive!!) {
                        // The hook is active, so trigger callbacks to report that recording has stopped.
                        val onScreenRecordingStopMethod = screenRecordingCallbackControllerClass.getDeclaredMethod("onScreenRecordingStop")
                        onScreenRecordingStopMethod.isAccessible = true
                        ScreenRecordingCallbackControllerHooker.allowMediaProjectionInfoCacheWipe = false
                        onScreenRecordingStopMethod.invoke(mScreenRecordingCallbackController)
                        ScreenRecordingCallbackControllerHooker.allowMediaProjectionInfoCacheWipe = true
                    }
                    else {
                        // The hook is inactive, so trigger callbacks to report that recording has started.
                        val onScreenRecordingStartMethod = screenRecordingCallbackControllerClass.getDeclaredMethod("onScreenRecordingStart",
                            classLoader!!.loadClass("android.media.projection.MediaProjectionInfo"))
                        onScreenRecordingStartMethod.isAccessible = true
                        onScreenRecordingStartMethod.invoke(mScreenRecordingCallbackController, null)
                    }
                }
            }
        }
    }
}