package com.keshav.capturesposed.hookers

import android.annotation.SuppressLint
import android.util.ArraySet
import com.keshav.capturesposed.BuildConfig
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam

object ScreenRecordingCallbackControllerHooker {
    private var module: XposedModule? = null
    private var classLoader: ClassLoader? = null

    @SuppressLint("PrivateApi")
    fun hook(param: SystemServerStartingParam, module: XposedModule) {
        this.module = module
        classLoader = param.classLoader

        val screenRecordingCallbackControllerClass =
            classLoader!!.loadClass("com.android.server.wm.ScreenRecordingCallbackController")

        val registerMethod = screenRecordingCallbackControllerClass.getDeclaredMethod(
            "register", classLoader!!.loadClass("android.window.IScreenRecordingCallback")
        )

        val dispatchCallbacksMethod = screenRecordingCallbackControllerClass.getDeclaredMethod(
            "dispatchCallbacks", ArraySet::class.java, Boolean::class.javaPrimitiveType
        )

        module.hook(registerMethod).intercept { chain ->
            val prefs = module.getRemotePreferences(BuildConfig.APPLICATION_ID)
            val isHookActive = prefs.getBoolean("screenRecordHookActive", true)

            /*
                register() returns a boolean indicating if the callback is associated with an app
                that is currently being recorded. When the hook is active, this method should be
                forced to return false.
             */
            val result = chain.proceed()
            if (isHookActive) false else result
        }

        module.hook(dispatchCallbacksMethod).intercept { chain ->
            val prefs = module.getRemotePreferences(BuildConfig.APPLICATION_ID)
            val isHookActive = prefs.getBoolean("screenRecordHookActive", true)

            if (isHookActive) {
                // If the screen recording detection block hook is active, then set the reported state to false.
                val args = chain.args.toTypedArray()
                args[1] = false
                chain.proceed(args)
            } else {
                chain.proceed()
            }
        }
    }
}
