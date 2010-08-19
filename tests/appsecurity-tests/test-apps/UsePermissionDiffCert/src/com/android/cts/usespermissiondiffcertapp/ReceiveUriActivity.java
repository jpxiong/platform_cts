package com.android.cts.usespermissiondiffcertapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.MessageQueue.IdleHandler;

public class ReceiveUriActivity extends Activity {
    private static final Object sLock = new Object();
    private static boolean sStarted;
    private static boolean sNewIntent;
    private static boolean sDestroyed;
    private static ReceiveUriActivity sCurInstance;

    Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        synchronized (sLock) {
            if (sCurInstance != null) {
                finishCurInstance();
            }
            sCurInstance = this;
            sStarted = true;
            sDestroyed = false;
            sLock.notifyAll();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        synchronized (sLock) {
            sNewIntent = true;
            sLock.notifyAll();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Looper.myQueue().addIdleHandler(new IdleHandler() {
            @Override
            public boolean queueIdle() {
                synchronized (sLock) {
                    sDestroyed = true;
                    sLock.notifyAll();
                }
                return false;
            }
        });
    }

    public static void finishCurInstance() {
        synchronized (sLock) {
            if (sCurInstance != null) {
                sCurInstance.finish();
                sCurInstance = null;
            }
        }
    }

    public static void finishCurInstanceSync() {
        finishCurInstance();

        synchronized (sLock) {
            final long startTime = SystemClock.uptimeMillis();
            while (!sDestroyed) {
                try {
                    sLock.wait(5000);
                } catch (InterruptedException e) {
                }
                if (SystemClock.uptimeMillis() >= (startTime+5000)) {
                    throw new RuntimeException("Timeout");
                }
            }
        }
    }

    public static void clearStarted() {
        synchronized (sLock) {
            sStarted = false;
        }
    }

    public static void clearNewIntent() {
        synchronized (sLock) {
            sNewIntent = false;
        }
    }

    public static void waitForStart() {
        synchronized (sLock) {
            final long startTime = SystemClock.uptimeMillis();
            while (!sStarted) {
                try {
                    sLock.wait(5000);
                } catch (InterruptedException e) {
                }
                if (SystemClock.uptimeMillis() >= (startTime+5000)) {
                    throw new RuntimeException("Timeout");
                }
            }
        }
    }

    public static void waitForNewIntent() {
        synchronized (sLock) {
            final long startTime = SystemClock.uptimeMillis();
            while (!sNewIntent) {
                try {
                    sLock.wait(5000);
                } catch (InterruptedException e) {
                }
                if (SystemClock.uptimeMillis() >= (startTime+5000)) {
                    throw new RuntimeException("Timeout");
                }
            }
        }
    }
}
