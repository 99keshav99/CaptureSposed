package com.keshav.capturesposed.hookers

import android.annotation.SuppressLint
import android.content.ComponentName
import android.os.Build
import android.os.ResultReceiver
import com.keshav.capturesposed.BuildConfig
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker
import java.io.FileDescriptor
import java.lang.reflect.Method

object WindowManagerServiceHooker {
    var module: XposedModule? = null
    var classLoader: ClassLoader? = null
    lateinit var registerScreenRecordingCallbackMethod: Method
    val screenRecordingCallbacks = mutableSetOf<Any>()

    @SuppressLint("PrivateApi")
    fun hook(param: SystemServerLoadedParam, module: XposedModule) {
        this.module = module
        classLoader = param.classLoader
        val windowManagerServiceClass = param.classLoader.loadClass("com.android.server.wm.WindowManagerService")
        registerScreenRecordingCallbackMethod = windowManagerServiceClass.getDeclaredMethod("registerScreenRecordingCallback",
            classLoader!!.loadClass("android.window.IScreenRecordingCallback"))

        module.hook(
            windowManagerServiceClass.getDeclaredMethod("notifyScreenshotListeners", Int::class.java),
            NotifyScreenshotListenersHooker::class.java
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            module.hook(registerScreenRecordingCallbackMethod, RegisterScreenRecordingCallbackHooker::class.java)

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
    private class RegisterScreenRecordingCallbackHooker: Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun beforeInvocation(callback: BeforeHookCallback) {
                module?.log("[CaptureSposed] WE ARE INSIDE THE REGISTRATION")
                screenRecordingCallbacks.add(callback.args[0])
                val prefs = module?.getRemotePreferences(BuildConfig.APPLICATION_ID)
                val isHookActive = prefs?.getBoolean("screenRecordHookActive", true)

                if (isHookActive!!)
                    callback.returnAndSkip(false)
            }
        }
    }

    @XposedHooker
    private class OnShellCommandHooker: Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun beforeInvocation(callback: BeforeHookCallback) {
                // This will intercept command: wm refresh-recording-callbacks
                val wmCommandArgs = callback.args[3] as Array<*>
                if (wmCommandArgs.size == 1 && wmCommandArgs[0] == "refresh-recording-callbacks") {
                    for (screenRecordingCallback in screenRecordingCallbacks) {
                        registerScreenRecordingCallbackMethod.invoke(callback.thisObject, screenRecordingCallback)
                    }
                }
            }
        }
    }
}