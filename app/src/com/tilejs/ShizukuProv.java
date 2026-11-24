package com.tilejs;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Where Shizuku's server drops its binder when this app starts (the same job as
 * the library's ShizukuProvider, without the extra dependencies it pulls in).
 */
public class ShizukuProv extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if ("sendBinder".equals(method) && extras != null) {
            try {
                extras.setClassLoader(getClass().getClassLoader());
                Parcelable container = extras.getParcelable(
                        "moe.shizuku.privileged.api.intent.extra.BINDER");
                if (container != null) {
                    Field f = container.getClass().getDeclaredField("binder");
                    f.setAccessible(true);
                    IBinder binder = (IBinder) f.get(container);
                    Method m = Class.forName("rikka.shizuku.Shizuku").getDeclaredMethod(
                            "onBinderReceived", IBinder.class, String.class);
                    m.setAccessible(true);
                    m.invoke(null, binder, getContext().getPackageName());
                }
            } catch (Throwable ignored) {
            }
        }
        return new Bundle();
    }

    @Override
    public Cursor query(Uri u, String[] p, String s, String[] a, String o) {
        return null;
    }

    @Override
    public String getType(Uri u) {
        return null;
    }

    @Override
    public Uri insert(Uri u, ContentValues v) {
        return null;
    }

    @Override
    public int delete(Uri u, String s, String[] a) {
        return 0;
    }

    @Override
    public int update(Uri u, ContentValues v, String s, String[] a) {
        return 0;
    }
}
