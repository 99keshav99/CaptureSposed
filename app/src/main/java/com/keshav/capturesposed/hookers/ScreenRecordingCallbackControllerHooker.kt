package com.keshav.capturesposed.hookers

import android.annotation.SuppressLint
import android.util.ArraySet
import android.util.Log
import com.keshav.capturesposed.BuildConfig
import com.keshav.capturesposed.TAG
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Method

object ScreenRecordingCallbackControllerHooker {
    private const val ID_REGISTER_SCREEN_RECORDING_CALLBACK = "registerScreenRecordingCallback"
    private const val ID_DISPATCH_CALLBACKS = "dispatchCallbacks"

    private val HOOK_IDS = setOf(ID_REGISTER_SCREEN_RECORDING_CALLBACK, ID_DISPATCH_CALLBACKS)

    private var module: XposedModule? = null
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

        val presentIds = mutableSetOf<String>()
        for (handle in oldHandles) {
            val id = handle.id ?: handle.executable.name
            if (id !in HOOK_IDS) {
                continue
            }
            val hooker = hookerForReplace(id, module) ?: continue
            handle.replaceHook(hooker)
            claimed.add(handle)
            presentIds.add(id)
        }

        val missing = HOOK_IDS - presentIds
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

        if (shouldInstall(ID_REGISTER_SCREEN_RECORDING_CALLBACK)) {
            try {
                val windowManagerServiceClass =
                    classLoader.loadClass("com.android.server.wm.WindowManagerService")
                hookMethod(
                    module,
                    registerScreenRecordingCallbackMethod(classLoader, windowManagerServiceClass),
                    ID_REGISTER_SCREEN_RECORDING_CALLBACK,
                    registerScreenRecordingCallbackHooker(module),
                )
                module.log(
                    Log.INFO,
                    TAG,
                    "Installed hook: WindowManagerService.registerScreenRecordingCallback",
                )
            } catch (e: Exception) {
                module.log(
                    Log.ERROR,
                    TAG,
                    "Failed to hook WindowManagerService.registerScreenRecordingCallback: $e",
                )
            }
        }

        if (shouldInstall(ID_DISPATCH_CALLBACKS)) {
            try {
                val screenRecordingCallbackControllerClass =
                    classLoader.loadClass("com.android.server.wm.ScreenRecordingCallbackController")
                hookMethod(
                    module,
                    dispatchCallbacksMethod(screenRecordingCallbackControllerClass),
                    ID_DISPATCH_CALLBACKS,
                    dispatchCallbacksHooker(module),
                )
                module.log(
                    Log.INFO,
                    TAG,
                    "Installed hook: ScreenRecordingCallbackController.dispatchCallbacks",
                )
            } catch (e: Exception) {
                module.log(
                    Log.ERROR,
                    TAG,
                    "Failed to hook ScreenRecordingCallbackController.dispatchCallbacks: $e",
                )
            }
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
            ID_REGISTER_SCREEN_RECORDING_CALLBACK -> registerScreenRecordingCallbackHooker(module)
            ID_DISPATCH_CALLBACKS -> dispatchCallbacksHooker(module)
            else -> null
        }

    // Shared reflection lookup for WindowManagerService.registerScreenRecordingCallback.
    @SuppressLint("PrivateApi")
    private fun registerScreenRecordingCallbackMethod(
        classLoader: ClassLoader,
        windowManagerServiceClass: Class<*>,
    ): Method = windowManagerServiceClass.getDeclaredMethod(
        "registerScreenRecordingCallback",
        classLoader.loadClass("android.window.IScreenRecordingCallback"),
    )

    // Shared reflection lookup for ScreenRecordingCallbackController.dispatchCallbacks.
    private fun dispatchCallbacksMethod(
        screenRecordingCallbackControllerClass: Class<*>,
    ): Method = screenRecordingCallbackControllerClass.getDeclaredMethod(
        "dispatchCallbacks", ArraySet::class.java, Boolean::class.javaPrimitiveType
    )

    // Interceptor for registerScreenRecordingCallback; extracted so the same logic is used on cold start and hot reload.
    private fun registerScreenRecordingCallbackHooker(module: XposedModule): XposedInterface.Hooker =
        { chain ->
            val prefs = module.getRemotePreferences(BuildConfig.APPLICATION_ID)
            val isHookActive = prefs.getBoolean("screenRecordHookActive", true)

            /*
                registerScreenRecordingCallback() returns a boolean indicating if the callback is
                associated with an app that is currently being recorded. When the hook is active,
                this method should be forced to return false.
             */
            val result = chain.proceed()
            if (isHookActive) false else result
        }

    // Interceptor for dispatchCallbacks; extracted so the same logic is used on cold start and hot reload.
    private fun dispatchCallbacksHooker(module: XposedModule): XposedInterface.Hooker = { chain ->
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
