package com.keshav.capturesposed

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.keshav.capturesposed.utils.PrefsUtils
import com.keshav.capturesposed.utils.XposedChecker

class QuickTile: TileService() {
    override fun onStartListening() {
        super.onStartListening()
        PrefsUtils.loadPrefs()
        setButtonState()
    }

    override fun onClick() {
        super.onClick()
        PrefsUtils.toggleHookState()
        setButtonState()
    }

    private fun setButtonState() {
        if (XposedChecker.isEnabled()) {
            if (PrefsUtils.isHookOn())
                qsTile.state = Tile.STATE_ACTIVE
            else
                qsTile.state = Tile.STATE_INACTIVE
        }
        else {
            qsTile.state = Tile.STATE_UNAVAILABLE
        }
        qsTile.updateTile()
    }
}