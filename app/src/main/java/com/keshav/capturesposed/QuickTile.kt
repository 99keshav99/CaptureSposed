package com.keshav.capturesposed

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.keshav.capturesposed.utils.PrefsUtils

class QuickTile: TileService() {
    override fun onStartListening() {
        super.onStartListening()
        PrefsUtils.loadPrefs()
        PrefsUtils.markTileRevealAsDone()
        setButtonState()
    }

    override fun onClick() {
        super.onClick()
        PrefsUtils.toggleHookState()
        setButtonState()
    }

    private fun setButtonState() {
        if (PrefsUtils.isHookOn())
            qsTile.state = Tile.STATE_ACTIVE
        else
            qsTile.state = Tile.STATE_INACTIVE
        qsTile.updateTile()
    }
}