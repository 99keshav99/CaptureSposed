package com.keshav.capturesposed.tiles

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.keshav.capturesposed.utils.PrefsUtils
import com.keshav.capturesposed.utils.XposedChecker

class ScreenshotQuickTile: TileService() {
    override fun onStartListening() {
        super.onStartListening()
        PrefsUtils.loadPrefs()
        PrefsUtils.markScreenshotTileRevealAsDone()
        setButtonState()
    }

    override fun onClick() {
        super.onClick()
        PrefsUtils.toggleScreenshotHookState()
        setButtonState()
    }

    private fun setButtonState() {
        if (XposedChecker.isEnabled()) {
            if (PrefsUtils.isScreenshotHookOn())
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