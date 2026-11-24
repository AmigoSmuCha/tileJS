package com.tilejs;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.service.quicksettings.TileService;

/**
 * Runs while a one-time full charge is active. Raises the phone's charge limit
 * to the target, watches the battery, and puts the old limit back when the
 * target is reached, or when the battery has gone well above 80% and then
 * fallen back to 81% (the token has been "spent").
 */
public class ChargeService extends Service {
    static final String CH = "charge";
    static final int ID = 1;
    static final String ACTION_START = "start";
    static final String ACTION_STOP = "stop";

    private final BroadcastReceiver battery = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            onBattery(i);
        }
    };
    private boolean registered;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(
                CH, "One-time full charge", NotificationManager.IMPORTANCE_LOW));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            finish(true, "turned off");
            return START_NOT_STICKY;
        }
        Notification n = note("Charging to " + L.chargeTarget(this) + "% once");
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(ID, n);
        }
        if (!registered) {
            registerReceiver(battery, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            registered = true;
        }
        return START_STICKY;
    }

    private void onBattery(Intent i) {
        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        if (level < 0 || scale <= 0) return;
        int pct = level * 100 / scale;
        int target = L.chargeTarget(this);

        int maxSeen = Math.max(L.maxSeen(this), pct);
        L.setMaxSeen(this, maxSeen);

        if (pct >= target) {
            finish(true, "reached " + target + "%");
            return;
        }
        // charged clearly above the 80% limit, then came back down to 81%
        if (maxSeen >= 85 && pct <= 81) {
            finish(true, "token spent (fell back to " + pct + "%)");
        }
    }

    /** Puts the saved limit back and stops. */
    private void finish(boolean restore, String why) {
        if (restore) {
            Agui.restore(L.savedLimit(this));
        }
        L.setChargeActive(this, false);
        L.setMaxSeen(this, 0);
        if (registered) {
            try {
                unregisterReceiver(battery);
            } catch (Exception ignored) {
            }
            registered = false;
        }
        TileService.requestListeningState(this,
                new android.content.ComponentName(this, ChargeTile.class));
        stopForeground(true);
        stopSelf();
    }

    private Notification note(String text) {
        Intent stop = new Intent(this, ChargeService.class).setAction(ACTION_STOP);
        android.app.PendingIntent pi = android.app.PendingIntent.getService(
                this, 0, stop, android.app.PendingIntent.FLAG_UPDATE_CURRENT
                        | android.app.PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CH)
                .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                .setContentTitle("tileJS")
                .setContentText(text)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Stop now", pi).build())
                .build();
    }

    /** Called by the tile: save the limit, raise it, start watching. */
    static void start(Context c) {
        L.saveLimit(c, Agui.read());
        L.setMaxSeen(c, 0);
        L.setChargeActive(c, true);
        Agui.setLimit(L.chargeTarget(c));
        Intent i = new Intent(c, ChargeService.class).setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }

    static void stop(Context c) {
        c.startService(new Intent(c, ChargeService.class).setAction(ACTION_STOP));
    }

    @Override
    public void onDestroy() {
        if (registered) {
            try {
                unregisterReceiver(battery);
            } catch (Exception ignored) {
            }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
