package com.tilejs;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;

/** Opens an activity from a tile and closes the quick panel (Android 14+ wants a PendingIntent). */
final class Open {
    private Open() {}

    static void from(TileService t, Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                t.startActivityAndCollapse(PendingIntent.getActivity(
                        t, 0, i, PendingIntent.FLAG_IMMUTABLE));
            } else {
                t.startActivityAndCollapse(i);
            }
        } catch (Throwable e) {
            try {
                t.startActivity(i);
            } catch (Throwable ignored) {
            }
        }
    }

    static void app(TileService t) {
        from(t, new Intent(t, MainActivity.class));
    }
}
