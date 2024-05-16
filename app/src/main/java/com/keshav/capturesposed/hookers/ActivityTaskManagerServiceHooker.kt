package com.keshav.capturesposed.hookers

import android.annotation.SuppressLint
import android.os.IBinder
import com.keshav.capturesposed.BuildConfig
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker

@XposedHooker
class ActivityTaskManagerServiceHooker {

    companion object {
        var module: XposedModule? = null

        @SuppressLint("PrivateApi")
        fun hook(param: SystemServerLoadedParam, module: XposedModule) {
            this.module = module
            module.hook(
                Class.forName(
                    "com.android.server.wm.ActivityTaskManagerService",
                    true,
                    param.classLoader
                ).getDeclaredMethod(
                    "registerScreenCaptureObserver",
                    IBinder::class.java,
                    Class.forName("android.app.IScreenCaptureObserver")
                ), RegisterScreenCaptureObserverHooker::class.java
            )
        }

        @XposedHooker
        private class RegisterScreenCaptureObserverHooker: Hooker {
            companion object {
                @Suppress("unused")
                @JvmStatic
                @BeforeInvocation
                fun beforeInvocation(callback: BeforeHookCallback) : RegisterScreenCaptureObserverHooker {
                    val prefs = module?.getRemotePreferences(BuildConfig.APPLICATION_ID)
                    val isHookActive = prefs?.getBoolean("hookActive", true)

                    if (isHookActive!!) {
                        module?.log("[CaptureSposed] Blocked screenshot detection.")
                        callback.returnAndSkip(null)
                    }
                    else {
                        module?.log("[CaptureSposed] Allowed screenshot detection.")
                    }

                    return RegisterScreenCaptureObserverHooker()
                }
            }
        }
    }
}