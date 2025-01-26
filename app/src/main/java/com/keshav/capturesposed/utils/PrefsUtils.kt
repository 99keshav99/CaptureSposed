package com.keshav.capturesposed.utils

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.keshav.capturesposed.BuildConfig
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

object PrefsUtils {
    private var prefs: SharedPreferences? = null
    private var screenshotHookActive: MutableLiveData<Boolean> = MutableLiveData<Boolean>(null)

    fun loadPrefs() {
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                XposedChecker.flagAsEnabled()
                prefs = service.getRemotePreferences(BuildConfig.APPLICATION_ID)
            }

            override fun onServiceDied(service: XposedService) {}
        })
    }

    fun isScreenshotHookOn(): Boolean {
        if (!XposedChecker.isEnabled()) {
            return false
        }

        if (screenshotHookActive.value == null) {
            screenshotHookActive.value = prefs!!.getBoolean("screenshotHookActive", true)
        }
        return screenshotHookActive.value as Boolean
    }

    fun toggleScreenshotHookState() {
        if (XposedChecker.isEnabled()) {
            screenshotHookActive.value = !isScreenshotHookOn()
            val prefEdit = prefs!!.edit()
            prefEdit.putBoolean("screenshotHookActive", screenshotHookActive.value!!)
            prefEdit.apply()
        }
    }

    fun markScreenshotTileRevealAsDone() {
        if (XposedChecker.isEnabled()) {
            val prefEdit = prefs!!.edit()
            prefEdit.putBoolean("tileRevealDone", true)
            prefEdit.apply()
        }
    }

    fun getScreenshotHookActiveAsLiveData(): MutableLiveData<Boolean> {
        return screenshotHookActive
    }
}