package com.tilejs;

import android.content.Context;
import android.provider.Settings;

/**
 * The two lights, through the same system settings the phone's own switches use
 * (found by watching them flip):
 *   front RGB  -> battery_charging_led, low_battery_led, notification_light_pulse,
 *                 incoming_led_color ("Red" / "None")
 *   back belt  -> led_belt_effect_enabled
 * Writing these needs the "Modify system settings" permission, granted in the app.
 */
final class Lights {
    private Lights() {}

    static boolean canWrite(Context c) {
        return Settings.System.canWrite(c);
    }

    // ---- front RGB ----

    static boolean frontOn(Context c) {
        return getInt(c, "battery_charging_led", 1) != 0;
    }

    static void setFront(Context c, boolean on) {
        if (!canWrite(c)) return;
        putInt(c, "battery_charging_led", on ? 1 : 0);
        putInt(c, "low_battery_led", on ? 1 : 0);
        putInt(c, "notification_light_pulse", on ? 1 : 0);
        try {
            Settings.System.putString(c.getContentResolver(),
                    "incoming_led_color", on ? "Red" : "None");
        } catch (Throwable ignored) {
        }
    }

    // ---- back LED belt ----

    static boolean backOn(Context c) {
        return getInt(c, "led_belt_effect_enabled", 0) != 0;
    }

    static void setBack(Context c, boolean on) {
        if (!canWrite(c)) return;
        putInt(c, "led_belt_effect_enabled", on ? 1 : 0);
    }

    private static int getInt(Context c, String key, int def) {
        try {
            return Settings.System.getInt(c.getContentResolver(), key, def);
        } catch (Throwable e) {
            return def;
        }
    }

    private static void putInt(Context c, String key, int v) {
        try {
            Settings.System.putInt(c.getContentResolver(), key, v);
        } catch (Throwable ignored) {
        }
    }
}
