package com.keshav.capturesposed.utils

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.keshav.capturesposed.BuildConfig
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

object PrefsUtils {
    private var prefs: SharedPreferences? = null
    private var screenshotHookActive: MutableLiveData<Boolean> = MutableLiveData<Boolean>(null)
    private var screenRecordHookActive: MutableLiveData<Boolean> = MutableLiveData<Boolean>(null)

    fun loadPrefs() {
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                XposedChecker.flagAsEnabled()
                prefs = service.getRemotePreferences(BuildConfig.APPLICATION_ID)

                // If the module does not have root, then turn off the hooks.
                if (!SuUtils.isRootAvailable()) {
                    setHookState(screenshotHookActive, "screenshotHookActive", false)
                    setHookState(screenRecordHookActive, "screenRecordHookActive", false)
                }
            }

            override fun onServiceDied(service: XposedService) {}
        })
    }

    fun isScreenshotHookOn(): Boolean {
        return isHookOn(screenshotHookActive, "screenshotHookActive")
    }

    fun isScreenRecordHookOn(): Boolean {
        return isHookOn(screenRecordHookActive, "screenRecordHookActive")
    }

    private fun isHookOn(liveData: MutableLiveData<Boolean>, prefKey: String): Boolean {
        if (!XposedChecker.isEnabled()) {
            return false
        }

        if (liveData.value == null) {
            liveData.value = prefs!!.getBoolean(prefKey, true)
        }
        return liveData.value as Boolean
    }

    fun toggleScreenshotHookState() {
        setHookState(screenshotHookActive, "screenshotHookActive", !isScreenshotHookOn())
    }

    fun toggleScreenRecordHookState() {
        setHookState(screenRecordHookActive, "screenRecordHookActive", !isScreenRecordHookOn())
        SuUtils.refreshRecordingCallbacks()
    }

    @SuppressLint("ApplySharedPref")
    private fun setHookState(liveData: MutableLiveData<Boolean>, prefKey: String, prefVal: Boolean) {
        if (XposedChecker.isEnabled()) {
            liveData.value = prefVal
            val prefEdit = prefs!!.edit()
            prefEdit.putBoolean(prefKey, prefVal)
            prefEdit.commit() // commit() is used instead of apply() to prevent a race condition.
        }
    }

    fun markTileRevealAsDone() {
        if (XposedChecker.isEnabled()) {
            val prefEdit = prefs!!.edit()
            prefEdit.putBoolean("tileRevealDone", true)
            prefEdit.apply()
        }
    }

    fun getScreenshotHookActiveAsLiveData(): MutableLiveData<Boolean> {
        return screenshotHookActive
    }

    fun getScreenRecordHookActiveAsLiveData(): MutableLiveData<Boolean> {
        return screenRecordHookActive
    }
}