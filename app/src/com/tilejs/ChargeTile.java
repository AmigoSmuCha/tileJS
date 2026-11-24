package com.tilejs;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Quick-settings tile: one-time full charge. */
public class ChargeTile extends TileService {
    @Override
    public void onStartListening() {
        refresh();
    }

    @Override
    public void onClick() {
        boolean active = L.chargeActive(this);
        if (active) {
            ChargeService.stop(this);
        } else if (!Agui.available()) {
            // can't reach the charge service: show it and open the app
            Tile t = getQsTile();
            if (t != null) {
                t.setState(Tile.STATE_UNAVAILABLE);
                t.updateTile();
            }
            startActivityAndCollapse(new android.content.Intent(this, MainActivity.class)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        } else {
            ChargeService.start(this);
        }
        refresh();
    }

    private void refresh() {
        Tile t = getQsTile();
        if (t == null) return;
        boolean active = L.chargeActive(this);
        if (!Agui.available()) {
            t.setState(Tile.STATE_UNAVAILABLE);
            t.setSubtitle("not supported");
        } else {
            t.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            t.setSubtitle(active ? "to " + L.chargeTarget(this) + "%" : "once to "
                    + L.chargeTarget(this) + "%");
        }
        t.setLabel("Full charge");
        try {
            t.setIcon(Icon.createWithResource(this, R.drawable.ic_charge));
        } catch (Throwable ignored) {
        }
        t.updateTile();
    }
}
