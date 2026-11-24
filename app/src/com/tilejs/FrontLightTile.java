package com.tilejs;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Quick-settings tile: the front RGB light on or off. */
public class FrontLightTile extends TileService {
    @Override
    public void onStartListening() {
        refresh();
    }

    @Override
    public void onClick() {
        if (!Lights.canWrite(this)) {
            Open.app(this);
            return;
        }
        Lights.setFront(this, !Lights.frontOn(this));
        refresh();
    }

    private void refresh() {
        Tile t = getQsTile();
        if (t == null) return;
        boolean can = Lights.canWrite(this);
        boolean on = can && Lights.frontOn(this);
        t.setState(!can ? Tile.STATE_UNAVAILABLE
                : on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.setLabel("Front light");
        try {
            t.setIcon(Icon.createWithResource(this, R.drawable.ic_front));
        } catch (Throwable ignored) {
        }
        t.updateTile();
    }
}
