package com.keshav.capturesposed.hookers

import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker

@XposedHooker
class ActivityTaskManagerServiceHooker : Hooker {

    companion object {
        @JvmStatic
        @BeforeInvocation
        @Suppress("unused")
        fun beforeInvocation(callback: BeforeHookCallback) {
            callback.returnAndSkip(null)
        }
    }
}