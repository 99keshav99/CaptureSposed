package com.keshav.capturesposed.utils

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.keshav.capturesposed.BuildConfig
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

object PrefsUtils {
    private var prefs: SharedPreferences? = null
    private var hookActive: MutableLiveData<Boolean> = MutableLiveData<Boolean>(null)

    fun loadPrefs() {
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                XposedChecker.flagAsEnabled()
                prefs = service.getRemotePreferences(BuildConfig.APPLICATION_ID)
            }

            override fun onServiceDied(service: XposedService) {}
        })
    }

    fun isHookOn(): Boolean {
        if (!XposedChecker.isEnabled()) {
            return false
        }

        if (hookActive.value == null) {
            hookActive.value = prefs!!.getBoolean("hookActive", true)
        }
        return hookActive.value as Boolean
    }

    fun toggleHookState() {
        if (XposedChecker.isEnabled()) {
            hookActive.value = !isHookOn()
            val prefEdit = prefs!!.edit()
            prefEdit.putBoolean("hookActive", hookActive.value!!)
            prefEdit.apply()
        }
    }

    fun markTileRevealAsDone() {
        if (XposedChecker.isEnabled()) {
            val prefEdit = prefs!!.edit()
            prefEdit.putBoolean("tileRevealDone", true)
            prefEdit.apply()
        }

    fun getHookActiveAsLiveData(): MutableLiveData<Boolean> {
        return hookActive
    }
}