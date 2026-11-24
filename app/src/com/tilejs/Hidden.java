package com.tilejs;

import java.lang.reflect.Method;

/**
 * Android hides android.os.ServiceManager from ordinary apps. This asks the VM
 * to stop hiding it, through the one call the VM still allows by reflection
 * (the long-standing "meta-reflection" exemption). Works up to Android 15/16.
 * If it ever fails, the service lookup just returns null and the app says so.
 */
final class Hidden {
    private static boolean done;

    private Hidden() {}

    static synchronized void allow() {
        if (done) return;
        done = true;
        try {
            Method forName = Class.class.getDeclaredMethod("forName", String.class);
            Method getDeclared = Class.class.getDeclaredMethod(
                    "getDeclaredMethod", String.class, Class[].class);
            Class<?> vmRuntime = (Class<?>) forName.invoke(null, "dalvik.system.VMRuntime");
            Method getRuntime = (Method) getDeclared.invoke(vmRuntime, "getRuntime", new Class[0]);
            Method setExemptions = (Method) getDeclared.invoke(vmRuntime,
                    "setHiddenApiExemptions", new Class[] {String[].class});
            Object runtime = getRuntime.invoke(null);
            setExemptions.invoke(runtime, new Object[] {new String[] {"L"}});
        } catch (Throwable ignored) {
        }
    }
}
