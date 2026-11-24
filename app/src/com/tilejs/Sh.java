package com.tilejs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

/**
 * Runs commands as the shell user through Shizuku (https://shizuku.rikka.app).
 * The phone's SELinux policy stops a normal app from using the charge service,
 * but the shell user may, and Shizuku lends this app that user.
 *
 * Everything goes through reflection so a different Shizuku API version can
 * only make a call fail, never break the build.
 */
final class Sh {
    static final int REQUEST_CODE = 7;
    static final String MANAGER = "moe.shizuku.privileged.api";

    private Sh() {}

    private static Class<?> api() {
        try {
            return Class.forName("rikka.shizuku.Shizuku");
        } catch (Throwable e) {
            return null;
        }
    }

    /** Shizuku is started and has handed this app its binder. */
    static boolean running() {
        try {
            return (Boolean) api().getMethod("pingBinder").invoke(null);
        } catch (Throwable e) {
            return false;
        }
    }

    /** Running, and the user allowed this app inside Shizuku. */
    static boolean granted() {
        try {
            return running()
                    && (Integer) api().getMethod("checkSelfPermission").invoke(null) == 0;
        } catch (Throwable e) {
            return false;
        }
    }

    /** Opens Shizuku's "allow this app?" prompt. */
    static void request() {
        try {
            api().getMethod("requestPermission", int.class).invoke(null, REQUEST_CODE);
        } catch (Throwable ignored) {
        }
    }

    /** Runs a command as shell, returns stdout (plus stderr), or null if it could not run. */
    static String run(String... cmd) {
        try {
            Method m = api().getDeclaredMethod("newProcess",
                    String[].class, String[].class, String.class);
            m.setAccessible(true);
            Process p = (Process) m.invoke(null, cmd, null, null);
            StringBuilder out = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) out.append(line).append('\n');
            r.close();
            BufferedReader e = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = e.readLine()) != null) out.append(line).append('\n');
            e.close();
            p.waitFor();
            return out.toString();
        } catch (Throwable e) {
            return null;
        }
    }
}
