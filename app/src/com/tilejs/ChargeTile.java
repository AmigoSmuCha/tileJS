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
            // no Shizuku (e.g. after a reboot): take the user to the phone's own
            // charge-limit screen so the limit can be changed by hand
            openChargeSettings();
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
            // inactive, not unavailable: Android ignores taps on unavailable tiles
            t.setState(Tile.STATE_INACTIVE);
            t.setSubtitle(Sh.running() ? "allow in Shizuku" : "Shizuku off - open settings");
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

    /** The phone's "battery health / charge limit" screen, or the nearest thing to it. */
    private void openChargeSettings() {
        android.content.Intent[] tries = {
                // the Unihertz fragment inside the Settings app
                new android.content.Intent()
                        .setClassName("com.android.settings", "com.android.settings.SubSettings")
                        .putExtra(":settings:show_fragment",
                                "com.agui.settings.fuelgauge.BatteryHealthSettings"),
                new android.content.Intent(android.content.Intent.ACTION_POWER_USAGE_SUMMARY),
                new android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
        };
        for (android.content.Intent i : tries) {
            i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                if (i.resolveActivity(getPackageManager()) == null) continue;
                startActivityAndCollapse(i);
                return;
            } catch (Throwable ignored) {
            }
        }
    }
}
