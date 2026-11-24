package com.tilejs;

import android.os.IBinder;
import android.os.Parcel;

import java.lang.reflect.Method;

/**
 * Talks to Unihertz's own system service "agui_common", the one the phone's
 * battery screen uses. Decompiled from agui-services.jar / agui-framework.jar:
 * the service's IACommonService interface (token "com.agui.ext.common.IACommonService")
 * has no permission or caller check, only these transactions:
 *   3 = getStringFromFile(key, def) -> String   (reads a stored value)
 *   4 = putStringToFile(key, value)              (writes a stored value)
 *   7 = setBatteryChargingStatus(boolean)        (applies the limit now)
 * The 80% limit is the stored value "key_charging_custom_mode" = "<percent>,<on>",
 * e.g. "80,true"; the system re-reads it on every battery change.
 *
 * Whether a normal app may reach this binder depends on the phone's SELinux
 * policy, which can't be read from the code, so every call reports success or
 * failure and MainActivity shows it.
 */
final class Agui {
    static final String SERVICE = "agui_common";
    static final String IFACE = "com.agui.ext.common.IACommonService";
    static final String KEY_CUSTOM = "key_charging_custom_mode";
    static final String KEY_INTELLIGENT = "key_charging_Intelligent_mode";

    private Agui() {}

    /** The service, or null if it can't be reached (then nothing here works). */
    static IBinder service() {
        Hidden.allow();
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager");
            Method get = sm.getMethod("getService", String.class);
            return (IBinder) get.invoke(null, SERVICE);
        } catch (Throwable e) {
            return null;
        }
    }

    /** Reachable through Shizuku (preferred, this phone blocks apps) or directly. */
    static boolean available() {
        return Sh.granted() || service() != null;
    }

    // ---- through Shizuku: `service call agui_common <code> ...` as the shell user ----

    /** The 32-bit words of a `service call` reply, or null if it failed. */
    private static long[] words(String out) {
        if (out == null) return null;
        java.util.ArrayList<Long> w = new java.util.ArrayList<>();
        boolean in = false;
        for (String line : out.split("\n")) {
            int p = line.indexOf("Parcel(");
            if (p >= 0) {
                in = true;
                line = line.substring(p + 7);
            }
            if (!in) continue;
            int q = line.indexOf('\'');
            if (q >= 0) line = line.substring(0, q);
            line = line.replaceAll("0x[0-9a-fA-F]+:", "").replace(")", "");
            for (String t : line.trim().split("\\s+")) {
                if (t.matches("[0-9a-fA-F]{8}")) w.add(Long.parseLong(t, 16));
            }
        }
        if (w.isEmpty()) return null;
        long[] r = new long[w.size()];
        for (int i = 0; i < r.length; i++) r[i] = w.get(i);
        return r;
    }

    private static boolean shellOk(String out) {
        long[] w = words(out);
        return w != null && w[0] == 0;
    }

    private static String shellString(String out, String def) {
        long[] w = words(out);
        if (w == null || w[0] != 0 || w.length < 2) return def;
        int len = (int) w[1];
        if (len < 0) return def;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < len; i++) {
            int idx = 2 + i / 2;
            if (idx >= w.length) break;
            b.append((char) ((w[idx] >> (16 * (i % 2))) & 0xFFFF));
        }
        return b.toString();
    }

    static String getString(String key, String def) {
        if (Sh.granted()) {
            return shellString(Sh.run("service", "call", SERVICE, "3",
                    "s16", key, "s16", def), def);
        }
        IBinder b = service();
        if (b == null) return def;
        Parcel data = Parcel.obtain(), reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(IFACE);
            data.writeString(key);
            data.writeString(def);
            b.transact(3, data, reply, 0);
            reply.readException();
            String s = reply.readString();
            return s == null ? def : s;
        } catch (Throwable e) {
            return def;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    /** True if the write went through without the binder refusing it. */
    static boolean putString(String key, String value) {
        if (Sh.granted()) {
            return shellOk(Sh.run("service", "call", SERVICE, "4",
                    "s16", key, "s16", value));
        }
        IBinder b = service();
        if (b == null) return false;
        Parcel data = Parcel.obtain(), reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(IFACE);
            data.writeString(key);
            data.writeString(value);
            b.transact(4, data, reply, 0);
            reply.readException();
            return true;
        } catch (Throwable e) {
            return false;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    static boolean setBatteryChargingStatus(boolean on) {
        if (Sh.granted()) {
            return shellOk(Sh.run("service", "call", SERVICE, "7",
                    "i32", on ? "1" : "0"));
        }
        IBinder b = service();
        if (b == null) return false;
        Parcel data = Parcel.obtain(), reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(IFACE);
            data.writeInt(on ? 1 : 0);
            b.transact(7, data, reply, 0);
            reply.readException();
            return true;
        } catch (Throwable e) {
            return false;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    // ---- the charge limit, in the phone's own format ----

    static final class Limit {
        final String intelligent;   // "true" / "false"
        final String custom;        // "<percent>,<on>"

        Limit(String intelligent, String custom) {
            this.intelligent = intelligent;
            this.custom = custom;
        }
    }

    /** Reads the limit exactly as the battery screen stores it, to put back later. */
    static Limit read() {
        return new Limit(getString(KEY_INTELLIGENT, "false"),
                getString(KEY_CUSTOM, "80,false"));
    }

    /** Sets a one-off custom limit at `percent` and applies it now. */
    static boolean setLimit(int percent) {
        boolean ok = putString(KEY_INTELLIGENT, "false");
        ok &= putString(KEY_CUSTOM, percent + ",true");
        ok &= setBatteryChargingStatus(true);
        return ok;
    }

    /** Puts a saved limit back and applies it. */
    static boolean restore(Limit saved) {
        boolean ok = putString(KEY_INTELLIGENT, saved.intelligent);
        ok &= putString(KEY_CUSTOM, saved.custom);
        ok &= setBatteryChargingStatus(true);
        return ok;
    }
}
