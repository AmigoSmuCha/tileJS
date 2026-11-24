package com.tilejs;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

/** Setup and status: permission, charge target, LED schedule, add the tiles. */
public class MainActivity extends Activity {
    private static final int BG = 0xFF201D1B;
    private static final int PANEL = 0xFF1B1B1B;
    private static final int ACCENT = 0xFFFE8019;
    private static final int INK = 0xFFD5C4A1;
    private static final int MUTED = 0xFF8E846E;

    private TextView status, chargeLabel, backLabel, crash;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        float dp = getResources().getDisplayMetrics().density;
        int pad = (int) (14 * dp);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad * 3, pad, pad * 3);
        root.setBackgroundColor(BG);

        TextView head = text(18, ACCENT, true);
        head.setText("tileJS");
        root.addView(head);
        status = text(12, INK, false);
        root.addView(status);

        root.addView(section("permission"));
        root.addView(button("allow modify system settings (for the lights)",
                new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                            Uri.parse("package:" + getPackageName())));
                } catch (Exception ignored) {
                }
            }
        }));

        root.addView(section("one-time full charge"));
        chargeLabel = text(12, INK, false);
        root.addView(chargeLabel);
        root.addView(button("allow tileJS in Shizuku (opens Shizuku)", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Sh.running()) {
                    Sh.request();
                } else {
                    Intent i = getPackageManager().getLaunchIntentForPackage(Sh.MANAGER);
                    if (i != null) {
                        startActivity(i);
                    } else {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/RikkaApps/Shizuku/releases")));
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }));
        LinearLayout targetRow = new LinearLayout(this);
        targetRow.setOrientation(LinearLayout.HORIZONTAL);
        targetRow.addView(button("-5%", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                L.setChargeTarget(MainActivity.this,
                        Math.max(85, L.chargeTarget(MainActivity.this) - 5));
                refresh();
            }
        }), half(dp));
        targetRow.addView(button("+5%", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                L.setChargeTarget(MainActivity.this,
                        Math.min(100, L.chargeTarget(MainActivity.this) + 5));
                refresh();
            }
        }), half(dp));
        root.addView(targetRow);

        root.addView(section("back LED schedule (used by the tile's 'scheduled' mode)"));
        backLabel = text(12, INK, false);
        root.addView(backLabel);
        LinearLayout onRow = new LinearLayout(this);
        onRow.setOrientation(LinearLayout.HORIZONTAL);
        onRow.addView(button("on -30m", shift(true, -30), dp), half(dp));
        onRow.addView(button("on +30m", shift(true, 30), dp), half(dp));
        root.addView(onRow);
        LinearLayout offRow = new LinearLayout(this);
        offRow.setOrientation(LinearLayout.HORIZONTAL);
        offRow.addView(button("off -30m", shift(false, -30), dp), half(dp));
        offRow.addView(button("off +30m", shift(false, 30), dp), half(dp));
        root.addView(offRow);

        root.addView(section("add the tiles to the quick panel"));
        TextView hint = text(11, MUTED, false);
        hint.setText(Build.VERSION.SDK_INT >= 33
                ? "Tap to add each tile, or pull down the panel, Edit, and drag them in."
                : "Pull down the quick panel, tap Edit (pencil), and drag these in: "
                        + "Full charge, Front light, Back LEDs.");
        root.addView(hint);
        if (Build.VERSION.SDK_INT >= 33) {
            root.addView(addButton("add Full charge tile", ChargeTile.class, "Full charge",
                    R.drawable.ic_charge));
            root.addView(addButton("add Front light tile", FrontLightTile.class, "Front light",
                    R.drawable.ic_front));
            root.addView(addButton("add Back LEDs tile", BackLightTile.class, "Back LEDs",
                    R.drawable.ic_back));
        }

        crash = text(10, MUTED, false);
        crash.setTextIsSelectable(true);
        root.addView(crash);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Schedule.apply(this);
        refresh();
    }

    private void refresh() {
        final boolean charge = Agui.available();
        boolean write = Lights.canWrite(this);
        String how = Sh.granted() ? "through Shizuku"
                : Sh.running() ? "Shizuku is running, tileJS is not allowed yet"
                : "Shizuku is not running";
        status.setText("charge limit service: " + (charge ? "reachable" : "NOT reachable")
                + "\n  " + how
                + "\nmodify system settings: " + (write ? "allowed" : "not allowed")
                + "\nandroid " + Build.VERSION.RELEASE);
        if (charge) {
            // read the real stored limit once, off the main thread, as proof it works
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String v = Agui.getString(Agui.KEY_CUSTOM, "?");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            chargeLabel.setText("Charges once to " + L.chargeTarget(
                                    MainActivity.this) + "%, then restores the saved limit."
                                    + "\nstored limit now: " + v);
                        }
                    });
                }
            }).start();
        } else {
            chargeLabel.setText("Install Shizuku, start it (wireless debugging or adb), "
                    + "then allow tileJS. The lights work without it.");
        }
        backLabel.setText(String.format(Locale.US, "scheduled: on at %s, off at %s",
                hhmm(L.backOnMinutes(this)), hhmm(L.backOffMinutes(this))));
        String report = L.crash(this);
        crash.setText(report.isEmpty() ? "" : "last crash:\n" + report);
        crash.setVisibility(report.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private static String hhmm(int m) {
        return String.format(Locale.US, "%02d:%02d", m / 60, m % 60);
    }

    private View.OnClickListener shift(final boolean on, final int delta) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int onMin = L.backOnMinutes(MainActivity.this);
                int offMin = L.backOffMinutes(MainActivity.this);
                if (on) onMin = wrap(onMin + delta);
                else offMin = wrap(offMin + delta);
                L.setBackTimes(MainActivity.this, onMin, offMin);
                Schedule.apply(MainActivity.this);
                refresh();
            }
        };
    }

    private static int wrap(int m) {
        return (m % 1440 + 1440) % 1440;
    }

    private TextView addButton(String label, final Class<?> tile, final String name,
                               final int icon) {
        return button(label, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 33) {
                    try {
                        getSystemService(android.app.StatusBarManager.class)
                                .requestAddTileService(
                                        new ComponentName(MainActivity.this, tile),
                                        name, android.graphics.drawable.Icon.createWithResource(
                                                MainActivity.this, icon),
                                        getMainExecutor(), r -> {});
                    } catch (Throwable ignored) {
                    }
                }
            }
        });
    }

    private TextView text(int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.MONOSPACE, bold ? Typeface.BOLD : Typeface.NORMAL);
        float dp = getResources().getDisplayMetrics().density;
        t.setPadding(0, (int) (6 * dp), 0, (int) (6 * dp));
        return t;
    }

    private TextView section(String name) {
        TextView t = text(11, MUTED, false);
        t.setText(name);
        float dp = getResources().getDisplayMetrics().density;
        t.setPadding(0, (int) (16 * dp), 0, (int) (2 * dp));
        return t;
    }

    private TextView button(String label, View.OnClickListener l) {
        float dp = getResources().getDisplayMetrics().density;
        TextView t = text(13, INK, true);
        t.setText(label);
        t.setGravity(Gravity.CENTER);
        t.setBackgroundColor(PANEL);
        int p = (int) (12 * dp);
        t.setPadding(p, p, p, p);
        t.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, (int) (4 * dp), 0, (int) (4 * dp));
        t.setLayoutParams(lp);
        return t;
    }

    private TextView button(String label, View.OnClickListener l, float dp) {
        return button(label, l);
    }

    private LinearLayout.LayoutParams half(float dp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
        int m = (int) (4 * dp);
        p.setMargins(m, m, m, m);
        return p;
    }
}
