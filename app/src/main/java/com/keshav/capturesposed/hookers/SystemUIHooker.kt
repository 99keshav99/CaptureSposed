package com.keshav.capturesposed.hookers

import android.annotation.SuppressLint
import android.os.Build
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
    private val includeScreenRecordTile = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
    private const val SCREENSHOT_TILE_ID = "custom(${BuildConfig.APPLICATION_ID}/.tiles.ScreenshotQuickTile)"
    private const val SCREEN_RECORD_TILE_ID = "custom(${BuildConfig.APPLICATION_ID}/.tiles.ScreenRecordQuickTile)"
    private var tilesAdded = false
    private var tilesRevealed = false

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
            if (!tilesAdded && !tilesRevealed) {
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
                    val addTileMethod = tileHostClass.getDeclaredMethod("addTile",
                        classLoader!!.loadClass("android.content.ComponentName"), Boolean::class.javaPrimitiveType)

                    val screenshotTileSpecObject = createMethod.invoke(null, SCREENSHOT_TILE_ID) as Any
                    val screenRecordTileSpecObject = createMethod.invoke(null, SCREEN_RECORD_TILE_ID) as Any

                    val screenshotTileComponentName = XposedHelpers.getObjectField(screenshotTileSpecObject, "componentName") as Any
                    val screenRecordTileComponentName = XposedHelpers.getObjectField(screenRecordTileSpecObject, "componentName") as Any

                    addTileMethod.invoke(tileHost, screenshotTileComponentName, true)

                    if (includeScreenRecordTile)
                        addTileMethod.invoke(tileHost, screenRecordTileComponentName, true)

                    module?.log("[CaptureSposed] Tiles added to quick settings panel.")
                }
                catch (t: Throwable) {
                    try {
                        val addTileMethod = tileHostClass.getDeclaredMethod("addTile", Int::class.java, String::class.java)
                        addTileMethod.invoke(tileHost, -1, SCREENSHOT_TILE_ID)

                        if (includeScreenRecordTile)
                            addTileMethod.invoke(tileHost, -1, SCREEN_RECORD_TILE_ID)

                        module?.log("[CaptureSposed] Tiles added to quick settings panel.")
                    }
                    catch (t: Throwable) {
                        val addTileMethod = tileHostClass.getDeclaredMethod("addTile", String::class.java, Int::class.java)
                        addTileMethod.invoke(tileHost, SCREENSHOT_TILE_ID, -1)

                        if (includeScreenRecordTile)
                            addTileMethod.invoke(tileHost, SCREEN_RECORD_TILE_ID, -1)

                        module?.log("[CaptureSposed] Tiles added to quick settings panel.")
                    }
                }
                tilesAdded = true
            }
        }
    }

    @XposedHooker
    private object TileRevealAnimHooker : Hooker {
        @JvmStatic
        @BeforeInvocation
        fun beforeInvocation(callback: BeforeHookCallback) {
            if (!tilesRevealed) {
                /*
                    Properly fixing the unchecked cast warning with Kotlin adds more performance overhead than it is
                    worth, so the warning is suppressed instead.
                 */
                @Suppress("UNCHECKED_CAST")
                val tilesToReveal = XposedHelpers.getObjectField(XposedHelpers.getSurroundingThis(callback.thisObject),
                    "mTilesToReveal") as ArraySet<String>
                tilesToReveal.add(SCREENSHOT_TILE_ID)

                if (includeScreenRecordTile)
                    tilesToReveal.add(SCREEN_RECORD_TILE_ID)

                tilesRevealed = true
                module?.log("[CaptureSposed] Tile quick settings panel animation played. CaptureSposed will not hook " +
                        "SystemUI on next reboot.")
            }
        }
    }
}