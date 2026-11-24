package com.tilejs;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Quick-settings tile: the back LEDs, cycling off -> on -> scheduled. */
public class BackLightTile extends TileService {
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
        L.setBackMode(this, (L.backMode(this) + 1) % 3);
        Schedule.apply(this);
        refresh();
    }

    private void refresh() {
        Tile t = getQsTile();
        if (t == null) return;
        boolean can = Lights.canWrite(this);
        int mode = L.backMode(this);
        if (!can) {
            t.setState(Tile.STATE_UNAVAILABLE);
            t.setSubtitle(null);
        } else {
            t.setState(mode == 0 ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
            t.setSubtitle(mode == 0 ? "off" : mode == 1 ? "on" : "scheduled");
        }
        t.setLabel("Back LEDs");
        try {
            t.setIcon(Icon.createWithResource(this, R.drawable.ic_back));
        } catch (Throwable ignored) {
        }
        t.updateTile();
    }
}
