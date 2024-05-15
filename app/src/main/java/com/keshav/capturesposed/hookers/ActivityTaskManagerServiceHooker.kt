package com.keshav.capturesposed.hookers

import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker

//private lateinit var captureSposed: CaptureSposed

@XposedHooker
class ActivityTaskManagerServiceHooker () : Hooker {

//    init {
//        captureSposed = module
//    }

    companion object {
        @JvmStatic
        @BeforeInvocation
        @Suppress("unused")
        fun beforeInvocation(callback: BeforeHookCallback) {
            callback.returnAndSkip(null)
//            if(PreferenceManager.isHookActive()) {
//                callback.returnAndSkip(null)
//            }
        }
    }
}