package com.keshav.capturesposed.utils

object XposedChecker {
    private var isEnabled = false

    fun flagAsEnabled() {
        isEnabled = true
    }

    fun isEnabled(): Boolean {
        return isEnabled
    }
}