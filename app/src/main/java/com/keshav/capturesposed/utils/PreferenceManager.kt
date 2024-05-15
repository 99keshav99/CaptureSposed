package com.keshav.capturesposed.utils

import android.content.Context
import android.content.SharedPreferences
import com.keshav.capturesposed.BuildConfig

class PreferenceManager {
    companion object {
        private const val KEY_HOOK_ACTIVE = "hookActive"
        private var preferences: SharedPreferences? = null

        fun loadPreferences(context: Context) {
            preferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
//            XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
//                override fun onServiceBind(service: XposedService) {
//                    preferences = service.getRemotePreferences(BuildConfig.APPLICATION_ID)
//                }
//
//                override fun onServiceDied(service: XposedService) {}
//            })
        }

        fun isHookActive(): Boolean {
            return preferences!!.getBoolean(KEY_HOOK_ACTIVE, false)
        }

        fun toggleHookState() {
            val hookState = !isHookActive()
            val editor = preferences!!.edit()
            editor.putBoolean(KEY_HOOK_ACTIVE, hookState)
            editor.apply()
        }
    }
}