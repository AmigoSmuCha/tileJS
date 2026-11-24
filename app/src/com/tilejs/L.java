package com.tilejs;

import android.content.Context;
import android.content.SharedPreferences;

/** Settings and a crash report, in SharedPreferences. */
final class L {
    private static final String PREFS = "tilejs";

    private L() {}

    static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // one-time full charge
    static int chargeTarget(Context c) {
        int v = p(c).getInt("charge_target", 95);
        return v >= 82 && v <= 100 ? v : 95;
    }

    static void setChargeTarget(Context c, int v) {
        p(c).edit().putInt("charge_target", v).apply();
    }

    static boolean chargeActive(Context c) {
        return p(c).getBoolean("charge_active", false);
    }

    static void setChargeActive(Context c, boolean on) {
        p(c).edit().putBoolean("charge_active", on).apply();
    }

    static int maxSeen(Context c) {
        return p(c).getInt("charge_max_seen", 0);
    }

    static void setMaxSeen(Context c, int v) {
        p(c).edit().putInt("charge_max_seen", v).apply();
    }

    static void saveLimit(Context c, Agui.Limit l) {
        p(c).edit().putString("saved_intel", l.intelligent)
                .putString("saved_custom", l.custom).apply();
    }

    static Agui.Limit savedLimit(Context c) {
        return new Agui.Limit(p(c).getString("saved_intel", "false"),
                p(c).getString("saved_custom", "80,false"));
    }

    // back LED belt: 0 off, 1 on, 2 scheduled
    static int backMode(Context c) {
        int v = p(c).getInt("back_mode", 0);
        return v >= 0 && v <= 2 ? v : 0;
    }

    static void setBackMode(Context c, int v) {
        p(c).edit().putInt("back_mode", v).apply();
    }

    static int backOnMinutes(Context c) {
        return p(c).getInt("back_on", 18 * 60);     // 18:00
    }

    static int backOffMinutes(Context c) {
        return p(c).getInt("back_off", 23 * 60);    // 23:00
    }

    static void setBackTimes(Context c, int on, int off) {
        p(c).edit().putInt("back_on", on).putInt("back_off", off).apply();
    }

    static String crash(Context c) {
        return p(c).getString("crash", "");
    }
}
