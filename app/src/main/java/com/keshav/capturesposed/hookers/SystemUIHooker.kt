package com.keshav.capturesposed.hookers

import android.annotation.SuppressLint
import com.keshav.capturesposed.BuildConfig
import com.keshav.capturesposed.utils.XposedHelpers
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker

object SystemUIHooker {
    var module: XposedModule? = null
    private const val tileId = "custom(${BuildConfig.APPLICATION_ID}/.QuickTile)"
    private var tileRevealed = false

    @SuppressLint("PrivateApi")
    fun hook(param: PackageLoadedParam, module: XposedModule) {
        this.module = module

        module.hook(
            param.classLoader.loadClass("com.android.systemui.qs.QSPanelControllerBase")
                .getDeclaredMethod("setTiles"),
            TileSetterHooker::class.java
        )

        /*module.hook(
            param.classLoader.loadClass("com.android.systemui.qs.QSTileRevealController\$1")
                .getDeclaredMethod("run"),
            TileRevealAnimHooker::class.java
        )*/
    }

    @XposedHooker
    private object TileSetterHooker : Hooker {
        @JvmStatic
        @BeforeInvocation
        fun beforeInvocation(callback: BeforeHookCallback) {
            if (!tileRevealed) {
                val tileHost = XposedHelpers.getObjectField(callback.thisObject, "mHost") as Any
                val tileHostClass = tileHost.javaClass as Class<*>

                // Depending on the Android distribution, the ordering of parameters is different.
                try {
                    tileHostClass.getDeclaredMethod("addTile", Int::class.java, String::class.java)
                        .invoke(tileHost, -1, tileId)
                }
                catch (t: Throwable) {
                    tileHostClass.getDeclaredMethod("addTile", String::class.java, Int::class.java)
                        .invoke(tileHost, tileId, -1)
                }

                module?.log("[CaptureSposed] Tile added to quick settings panel.")
            }
        }
    }
}