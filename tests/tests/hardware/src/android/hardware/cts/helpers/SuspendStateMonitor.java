package android.hardware.cts.helpers;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.hardware.Sensor;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import junit.framework.Assert;

public class SuspendStateMonitor {
    private final double firstRealTimeMillis;
    private final double firstUpTimeMillis;
    private double lastSleepTimeSeconds = 0;
    private volatile long lastWakeUpTime = 0;
    Timer sleepMonitoringTimer = new Timer();

    /**
     * Returns the time the device slept since the start of the application,
     * in seconds.
     */
    public double getSleepTimeSeconds() {
        double totalSinceStart = android.os.SystemClock.elapsedRealtime() - firstRealTimeMillis;
        double upTimeSinceStart = android.os.SystemClock.uptimeMillis() - firstUpTimeMillis;
        return (totalSinceStart - upTimeSinceStart) / 1000;
    }

    public long getLastWakeUpTime() {
        return lastWakeUpTime;
    }

    public void cancel() {
        sleepMonitoringTimer.cancel();
    }

     public SuspendStateMonitor() {
        firstRealTimeMillis = android.os.SystemClock.elapsedRealtime();
        firstUpTimeMillis = android.os.SystemClock.uptimeMillis();
        // Every 100 miliseconds, check whether the device has slept.
        TimerTask sleepMonitoringTask = new TimerTask() {
                @Override
                public void run() {
                    if (getSleepTimeSeconds() - lastSleepTimeSeconds > 0.1) {
                        lastSleepTimeSeconds = getSleepTimeSeconds();
                        lastWakeUpTime = SystemClock.elapsedRealtime();
                    }
                }
        };
        sleepMonitoringTimer.schedule(sleepMonitoringTask, 0, 100);
    }
}
