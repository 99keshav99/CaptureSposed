package com.keshav.capturesposed.hookers

import android.annotation.SuppressLint
import android.util.ArraySet
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
    var classLoader: ClassLoader? = null
    private const val SCREENSHOT_TILE_ID = "custom(${BuildConfig.APPLICATION_ID}/.tiles.ScreenshotQuickTile)"
    private var tileRevealed = false

    @SuppressLint("PrivateApi")
    fun hook(param: PackageLoadedParam, module: XposedModule) {
        this.module = module
        classLoader = param.classLoader

        module.hook(
            param.classLoader.loadClass("com.android.systemui.qs.QSPanelControllerBase")
                .getDeclaredMethod("setTiles"),
            TileSetterHooker::class.java
        )

        module.hook(
            param.classLoader.loadClass("com.android.systemui.qs.QSTileRevealController\$1")
                .getDeclaredMethod("run"),
            TileRevealAnimHooker::class.java
        )
    }

    @XposedHooker
    private object TileSetterHooker : Hooker {
        @SuppressLint("PrivateApi")
        @JvmStatic
        @BeforeInvocation
        fun beforeInvocation(callback: BeforeHookCallback) {
            if (!tileRevealed) {
                val tileHost = XposedHelpers.getObjectField(callback.thisObject, "mHost") as Any
                val tileHostClass = tileHost.javaClass as Class<*>

                /*
                    The range of supported Android versions for CaptureSposed (14 through 15 QPR 1 as of this comment) use
                    several different approaches for adding tiles to the tile drawer. This collection of try-catch blocks
                    accounts for the different approaches that are used. Ideally, using conditional checks to identify the
                    Android version would be preferred, but since different OEMs may use different variations across the same
                    Android version, using try-catch blocks is safer.
                 */
                try {
                    val tileSpecClass = classLoader!!.loadClass("com.android.systemui.qs.pipeline.shared.TileSpec\$Companion")
                    val createMethod = tileSpecClass.getDeclaredMethod("create", String::class.java)
                    val tileSpecObject = createMethod.invoke(null, SCREENSHOT_TILE_ID) as Any
                    val componentName = XposedHelpers.getObjectField(tileSpecObject, "componentName") as Any

                    tileHostClass.getDeclaredMethod("addTile",
                        classLoader!!.loadClass("android.content.ComponentName"),
                        Boolean::class.javaPrimitiveType)
                        .invoke(tileHost, componentName, true)

                    module?.log("[CaptureSposed] Tile added to quick settings panel.")
                }
                catch (t: Throwable) {
                    try {
                        tileHostClass.getDeclaredMethod("addTile", Int::class.java, String::class.java)
                            .invoke(tileHost, -1, SCREENSHOT_TILE_ID)
                        module?.log("[CaptureSposed] Tile added to quick settings panel.")
                    }
                    catch (t: Throwable) {
                        tileHostClass.getDeclaredMethod("addTile", String::class.java, Int::class.java)
                            .invoke(tileHost, SCREENSHOT_TILE_ID, -1)
                        module?.log("[CaptureSposed] Tile added to quick settings panel.")
                    }
                }
            }
        }
    }

    @XposedHooker
    private object TileRevealAnimHooker : Hooker {
        @JvmStatic
        @BeforeInvocation
        fun beforeInvocation(callback: BeforeHookCallback) {
            if (!tileRevealed) {
                /*
                    Properly fixing the unchecked cast warning with Kotlin adds more performance overhead than it is
                    worth, so the warning is suppressed instead.
                 */
                @Suppress("UNCHECKED_CAST")
                val tilesToReveal = XposedHelpers.getObjectField(XposedHelpers.getSurroundingThis(callback.thisObject),
                    "mTilesToReveal") as ArraySet<String>
                tilesToReveal.add(SCREENSHOT_TILE_ID)
                tileRevealed = true
                module?.log("[CaptureSposed] Tile quick settings panel animation played. CaptureSposed will not hook " +
                        "SystemUI on next reboot.")
            }
        }
    }
}