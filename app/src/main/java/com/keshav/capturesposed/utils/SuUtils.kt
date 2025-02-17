package com.keshav.capturesposed.utils

import com.topjohnwu.superuser.Shell

object SuUtils {
    // Checks if root is available and prompts for access on platforms that offer it if appropriate.
    fun isRootAvailable(): Boolean {
        val result = Shell.cmd("whoami").exec()
        return result.isSuccess && Shell.getShell().status == Shell.ROOT_SHELL
    }

    /*
        Triggers our injected window manager command to refresh the state of the screen recording
        callbacks.
     */
    fun refreshRecordingCallbacks() {
        Shell.cmd("wm refresh-recording-callbacks").exec()
    }
}