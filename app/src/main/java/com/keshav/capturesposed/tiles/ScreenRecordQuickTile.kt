package com.keshav.capturesposed.tiles

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.keshav.capturesposed.utils.PrefsUtils
import com.keshav.capturesposed.utils.XposedChecker

class ScreenRecordQuickTile: TileService() {
    override fun onStartListening() {
        super.onStartListening()
        PrefsUtils.loadPrefs()
        PrefsUtils.markTileRevealAsDone()
        setButtonState()
    }

    override fun onClick() {
        super.onClick()
        PrefsUtils.toggleScreenRecordHookState()
        setButtonState()
    }

    private fun setButtonState() {
        if (XposedChecker.isEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            if (PrefsUtils.isScreenRecordHookOn())
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