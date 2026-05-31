package com.keshav.capturesposed.hookers

import android.annotation.SuppressLint
import android.util.ArraySet
import com.keshav.capturesposed.BuildConfig
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam

object ScreenRecordingCallbackControllerHooker {
    private var module: XposedModule? = null
    private var classLoader: ClassLoader? = null

    @SuppressLint("PrivateApi")
    fun hook(param: SystemServerLoadedParam, module: XposedModule) {
        this.module = module
        classLoader = param.classLoader

        val screenRecordingCallbackControllerClass = classLoader!!.loadClass("com.android.server.wm.ScreenRecordingCallbackController")

        val registerMethod = screenRecordingCallbackControllerClass.getDeclaredMethod("register",
            classLoader!!.loadClass("android.window.IScreenRecordingCallback"))

        val dispatchCallbacksMethod = screenRecordingCallbackControllerClass.getDeclaredMethod("dispatchCallbacks",
            ArraySet::class.java, Boolean::class.javaPrimitiveType)

        module.hook(registerMethod, RegisterHooker::class.java)
        module.hook(dispatchCallbacksMethod, DispatchCallbacksHooker::class.java)
    }

    private class RegisterHooker: Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                val prefs = module?.getRemotePreferences(BuildConfig.APPLICATION_ID)
                val isHookActive = prefs?.getBoolean("screenRecordHookActive", true)

                /*
                    register() returns a boolean indicating if the callback is associated with an app
                    that is currently being recorded. When the hook is active, this method should be
                    forced to return false.
                 */
                if (isHookActive!!)
                    callback.result = false
            }
        }
    }

    private class DispatchCallbacksHooker: Hooker {
        companion object {
            @JvmStatic
            fun before(callback: BeforeHookCallback) {
                val prefs = module?.getRemotePreferences(BuildConfig.APPLICATION_ID)
                val isHookActive = prefs?.getBoolean("screenRecordHookActive", true)

                if (isHookActive!!) {
                    // If the screen recording detection block hook is active, then set the reported state to false.
                    callback.args[1] = false
                }
            }
        }
    }
}