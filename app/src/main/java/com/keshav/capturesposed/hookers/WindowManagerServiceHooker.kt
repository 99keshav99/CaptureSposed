package com.keshav.capturesposed.hookers

import android.annotation.SuppressLint
import android.content.ComponentName
import android.os.Build
import android.os.ResultReceiver
import android.util.ArraySet
import android.util.Log
import com.keshav.capturesposed.BuildConfig
import com.keshav.capturesposed.TAG
import com.keshav.capturesposed.utils.XposedHelpers
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.io.FileDescriptor
import java.lang.reflect.Method

object WindowManagerServiceHooker {
    private const val ID_NOTIFY_SCREENSHOT_LISTENERS = "notifyScreenshotListeners"
    private const val ID_ON_SHELL_COMMAND = "onShellCommand"

    private fun hookIds(): Set<String> = buildSet {
        add(ID_NOTIFY_SCREENSHOT_LISTENERS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            add(ID_ON_SHELL_COMMAND)
        }
    }

    var module: XposedModule? = null
    private var classLoader: ClassLoader? = null

    // Clears references to the old module generation during onHotReloading so this object does not pin it.
    fun clearGenerationRefs() {
        module = null
        classLoader = null
    }

    fun hook(classLoader: ClassLoader, module: XposedModule) {
        this.module = module
        this.classLoader = classLoader
        installHooks(classLoader, module, null)
    }

    // Hot reload path: atomically swap old interceptors for new ones and install any missing hooks.
    fun replaceHooks(
        oldHandles: List<XposedInterface.HookHandle>,
        module: XposedModule,
    ): Set<XposedInterface.HookHandle> {
        this.module = module
        val claimed = mutableSetOf<XposedInterface.HookHandle>()
        if (module.getApiVersion() < XposedInterface.API_102) {
            return claimed
        }

        val ids = hookIds()
        val presentIds = mutableSetOf<String>()
        for (handle in oldHandles) {
            val id = handle.id ?: handle.executable.name
            if (id !in ids) {
                continue
            }
            val hooker = hookerForReplace(id, module) ?: continue
            handle.replaceHook(hooker)
            claimed.add(handle)
            presentIds.add(id)
        }

        val missing = ids - presentIds
        if (missing.isNotEmpty() && oldHandles.isNotEmpty()) {
            val loader = oldHandles.first().executable.declaringClass.classLoader
                ?: return claimed
            this.classLoader = loader
            installHooks(loader, module, missing)
        }
        return claimed
    }

    @SuppressLint("PrivateApi")
    private fun installHooks(
        classLoader: ClassLoader,
        module: XposedModule,
        onlyIds: Set<String>?,
    ) {
        fun shouldInstall(id: String) = onlyIds == null || id in onlyIds

        val windowManagerServiceClass =
            classLoader.loadClass("com.android.server.wm.WindowManagerService")

        if (shouldInstall(ID_NOTIFY_SCREENSHOT_LISTENERS)) {
            val method = windowManagerServiceClass.getDeclaredMethod(
                "notifyScreenshotListeners", Int::class.java
            )
            hookMethod(
                module,
                method,
                ID_NOTIFY_SCREENSHOT_LISTENERS,
                notifyScreenshotListenersHooker(module),
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
            shouldInstall(ID_ON_SHELL_COMMAND)
        ) {
            hookMethod(
                module,
                onShellCommandMethod(classLoader, windowManagerServiceClass),
                ID_ON_SHELL_COMMAND,
                onShellCommandHooker(),
            )
        }
    }

    private fun hookMethod(
        module: XposedModule,
        method: Method,
        id: String,
        hooker: XposedInterface.Hooker,
    ) {
        val builder = module.hook(method)
        /*
            setId is API 102-only; on API 101 frameworks the compile-time API is still 102 but
            getApiVersion() returns the framework LIB_API so this guard is a real 101/102 split.
         */
        if (module.getApiVersion() >= XposedInterface.API_102) {
            builder.setId(id)
        }
        builder.intercept(hooker)
    }

    private fun hookerForReplace(id: String, module: XposedModule): XposedInterface.Hooker? =
        when (id) {
            ID_NOTIFY_SCREENSHOT_LISTENERS -> notifyScreenshotListenersHooker(module)
            ID_ON_SHELL_COMMAND -> onShellCommandHooker()
            else -> null
        }

    // Shared reflection lookup for WindowManagerService.onShellCommand (API 35+).
    @SuppressLint("PrivateApi")
    private fun onShellCommandMethod(
        classLoader: ClassLoader,
        windowManagerServiceClass: Class<*>,
    ): Method = windowManagerServiceClass.getDeclaredMethod(
        "onShellCommand",
        FileDescriptor::class.java,
        FileDescriptor::class.java,
        FileDescriptor::class.java,
        Array<String>::class.java,
        classLoader.loadClass("android.os.ShellCallback"),
        ResultReceiver::class.java,
    )

    // Interceptor for notifyScreenshotListeners; extracted so the same logic is used on cold start and hot reload.
    private fun notifyScreenshotListenersHooker(module: XposedModule): XposedInterface.Hooker =
        { chain ->
            val prefs = module.getRemotePreferences(BuildConfig.APPLICATION_ID)
            val isHookActive = prefs.getBoolean("screenshotHookActive", true)

            if (isHookActive) {
                module.log(Log.INFO, TAG, "Blocked screenshot detection.")
                emptyList<ComponentName>()
            } else {
                module.log(Log.INFO, TAG, "Allowed screenshot detection.")
                chain.proceed()
            }
        }

    // Interceptor for onShellCommand; extracted so the same logic is used on cold start and hot reload.
    private fun onShellCommandHooker(): XposedInterface.Hooker = { chain ->
        // This will intercept command: wm refresh-recording-callbacks
        val wmCommandArgs = chain.getArg(3) as Array<*>
        if (wmCommandArgs.size == 1 && wmCommandArgs[0] == "refresh-recording-callbacks") {
            val mScreenRecordingCallbackController = XposedHelpers.getObjectField(
                chain.thisObject, "mScreenRecordingCallbackController"
            ) as Any
            val screenRecordingCallbackControllerClass = mScreenRecordingCallbackController::class.java

            val getRecordedUidsMethod =
                screenRecordingCallbackControllerClass.getDeclaredMethod("getRecordedUids")
            val dispatchCallbacksMethod = screenRecordingCallbackControllerClass.getDeclaredMethod(
                "dispatchCallbacks", ArraySet::class.java, Boolean::class.javaPrimitiveType
            )

            getRecordedUidsMethod.isAccessible = true
            dispatchCallbacksMethod.isAccessible = true

            val recordedUids = getRecordedUidsMethod.invoke(mScreenRecordingCallbackController)
            val mRecordedWC =
                XposedHelpers.getObjectField(mScreenRecordingCallbackController, "mRecordedWC")

            dispatchCallbacksMethod.invoke(
                mScreenRecordingCallbackController, recordedUids, mRecordedWC != null
            )
        }
        chain.proceed()
    }
}
