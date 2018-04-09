package com.qmuiteam.qmui.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by Bruce Too
 * On 09/04/2018.
 * At 14:29
 */
public class QMUILogger {

    //TODO Config debug or not
    public static final boolean DEBUG = true;

    public static void printStack(@Nullable Throwable throwable) {
        if (DEBUG) {
            Log.e("Stack", Log.getStackTraceString(throwable));
        }
    }

    public static void v(@NonNull String tag, @NonNull String msg) {
        if (DEBUG) {
            Log.v(tag, msg);
        }
    }

    public static void v(@NonNull String tag, @NonNull String msg, Throwable e) {
        if (DEBUG) {
            Log.v(tag, msg, e);
        }
    }

    public static void d(@NonNull String tag, @NonNull String msg) {
        if (DEBUG) {
            Log.d(tag, msg);
        }
    }

    public static void d(@NonNull String tag, @NonNull String msg, Throwable e) {
        if (DEBUG) {
            Log.d(tag, msg, e);
        }
    }

    public static void i(@NonNull String tag, @NonNull String msg) {
        if (DEBUG) {
            Log.i(tag, msg);
        }
    }

    public static void i(@NonNull String tag, @NonNull String msg, Throwable e) {
        if (DEBUG) {
            Log.i(tag, msg, e);
        }
    }

    public static void w(@NonNull String tag, @NonNull String msg) {
        if (DEBUG) {
            Log.w(tag, msg);
        }
    }

    public static void w(@NonNull String tag, Throwable e) {
        if (DEBUG) {
            Log.w(tag, e);
        }
    }

    public static void w(@NonNull String tag, @NonNull String msg, Throwable e) {
        if (DEBUG) {
            Log.w(tag, msg, e);
        }
    }

    public static void e(@NonNull String tag, @NonNull String msg) {
        if (DEBUG) {
            Log.e(tag, msg);
        }
    }

    public static void e(@NonNull String tag, @NonNull String msg, Throwable e) {
        if (DEBUG) {
            Log.e(tag, msg, e);
        }
    }

    public static void wtf(@NonNull String tag, @NonNull String msg) {
        if (DEBUG) {
            Log.wtf(tag, msg);
        }
    }
}
