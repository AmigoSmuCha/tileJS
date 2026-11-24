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
        put(c, "battery_charging_led", on ? "1" : "0");
        put(c, "low_battery_led", on ? "1" : "0");
        put(c, "notification_light_pulse", on ? "1" : "0");
        // turning off remembers the colour, turning on puts it back
        String now = Settings.System.getString(c.getContentResolver(), "incoming_led_color");
        if (!on && now != null && !now.equals("None")) {
            L.p(c).edit().putString("front_color", now).apply();
        }
        put(c, "incoming_led_color", on ? L.p(c).getString("front_color", "Green") : "None");
    }

    // ---- back LED belt ----

    static boolean backOn(Context c) {
        return getInt(c, "led_belt_effect_enabled", 0) != 0;
    }

    static void setBack(Context c, boolean on) {
        if (!canWrite(c)) return;
        put(c, "led_belt_effect_enabled", on ? "1" : "0");
    }

    private static int getInt(Context c, String key, int def) {
        try {
            return Settings.System.getInt(c.getContentResolver(), key, def);
        } catch (Throwable e) {
            return def;
        }
    }

    /**
     * Writes a system setting, checks by reading it back, and if the phone refused
     * it falls back to the shell through Shizuku. The outcome is kept for the app.
     */
    private static void put(Context c, String key, String value) {
        String err = null;
        try {
            Settings.System.putString(c.getContentResolver(), key, value);
        } catch (Throwable e) {
            err = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        if (value.equals(Settings.System.getString(c.getContentResolver(), key))) {
            L.p(c).edit().putString("light_err", "").apply();
            return;
        }
        if (Sh.granted()) {
            Sh.run("settings", "put", "system", key, value);
            if (value.equals(Settings.System.getString(c.getContentResolver(), key))) {
                L.p(c).edit().putString("light_err", "").apply();
                return;
            }
        }
        L.p(c).edit().putString("light_err", key + " not changed"
                + (err == null ? " (write ignored)" : " (" + err + ")")
                + (Sh.granted() ? "; shell too" : "; Shizuku not available")).apply();
    }
}
