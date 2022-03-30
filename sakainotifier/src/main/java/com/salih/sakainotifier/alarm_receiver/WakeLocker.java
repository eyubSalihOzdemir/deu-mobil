package com.salih.sakainotifier.alarm_receiver;
import android.content.Context;
import android.os.PowerManager;

public abstract class WakeLocker {
    private static PowerManager.WakeLock wakeLock;

    public static void acquire(Context ctx) {
        if (wakeLock != null) wakeLock.release();

        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, "AppName:tag");
        wakeLock.acquire(10*60*1000L /*10 minutes*/);
    }

    public static void release() {
        if (wakeLock != null) wakeLock.release(); wakeLock = null;
    }
}