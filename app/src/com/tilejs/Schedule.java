package com.tilejs;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/**
 * The back LED belt's schedule. Two daily alarms (on at one time, off at
 * another) flip led_belt_effect_enabled, so the app does not run in between.
 * Also re-armed at boot.
 */
public class Schedule extends BroadcastReceiver {
    static final String ACTION_ON = "com.tilejs.LED_ON";
    static final String ACTION_OFF = "com.tilejs.LED_OFF";

    @Override
    public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) {
            apply(c);
        } else if (ACTION_ON.equals(a)) {
            if (L.backMode(c) == 2) Lights.setBack(c, true);
        } else if (ACTION_OFF.equals(a)) {
            if (L.backMode(c) == 2) Lights.setBack(c, false);
        }
    }

    /** Sets the belt for the current mode and arms or cancels the daily alarms. */
    static void apply(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent on = pi(c, ACTION_ON);
        PendingIntent off = pi(c, ACTION_OFF);
        am.cancel(on);
        am.cancel(off);

        int mode = L.backMode(c);
        if (mode == 1) {
            Lights.setBack(c, true);
        } else if (mode == 0) {
            Lights.setBack(c, false);
        } else {
            int onMin = L.backOnMinutes(c), offMin = L.backOffMinutes(c);
            daily(am, on, onMin);
            daily(am, off, offMin);
            // set the right state for right now
            Lights.setBack(c, within(onMin, offMin));
        }
    }

    private static boolean within(int onMin, int offMin) {
        int now = nowMinutes();
        if (onMin == offMin) return false;
        if (onMin < offMin) return now >= onMin && now < offMin;
        return now >= onMin || now < offMin;      // wraps past midnight
    }

    private static int nowMinutes() {
        Calendar n = Calendar.getInstance();
        return n.get(Calendar.HOUR_OF_DAY) * 60 + n.get(Calendar.MINUTE);
    }

    private static void daily(AlarmManager am, PendingIntent pi, int minutes) {
        Calendar when = Calendar.getInstance();
        when.set(Calendar.HOUR_OF_DAY, minutes / 60);
        when.set(Calendar.MINUTE, minutes % 60);
        when.set(Calendar.SECOND, 0);
        when.set(Calendar.MILLISECOND, 0);
        if (when.getTimeInMillis() <= System.currentTimeMillis()) {
            when.add(Calendar.DAY_OF_YEAR, 1);
        }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pi);
    }

    private static PendingIntent pi(Context c, String action) {
        return PendingIntent.getBroadcast(c, action.hashCode(),
                new Intent(c, Schedule.class).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
