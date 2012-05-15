/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.cts.verifier.location;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

public class LocationVerifier implements LocationListener, Handler.Callback {
    public static final String TAG = "CtsVerifierLocation";

    private static final int MSG_TIMEOUT = 1;

    private final LocationManager mLocationManager;
    private final PassFailLog mCb;
    private final String mProvider;
    private final long mInterval;
    private final long mMinInterval;
    private final long mMaxInterval;
    private final Handler mHandler;
    private final int mRequestedUpdates;

    private long mLastTimestamp = -1;
    private int mNumUpdates = 0;
    private boolean mRunning = false;

    public LocationVerifier(PassFailLog cb, LocationManager locationManager,
            String provider, long requestedInterval, int numUpdates) {
        mProvider = provider;
        mInterval = requestedInterval;
        // Updates can be up to 100ms fast
        mMinInterval = Math.max(0, requestedInterval - 100);
        // timeout at 60 seconds
        mMaxInterval = requestedInterval + 60 * 1000;
        mRequestedUpdates = numUpdates;
        mLocationManager = locationManager;
        mCb = cb;
        mHandler = new Handler(this);
    }

    public void start() {
        mCb.log("enabling " + mProvider + " for " + mInterval + "ms updates");
        mRunning = true;
        expectNextUpdate(mMaxInterval);
        mLastTimestamp = SystemClock.elapsedRealtime();
        mLocationManager.requestLocationUpdates(mProvider, mInterval, 0,
                LocationVerifier.this);
    }

    public void stop() {
        mRunning = false;
        mLocationManager.removeUpdates(LocationVerifier.this);
        mHandler.removeMessages(MSG_TIMEOUT);
    }

    private void pass() {
        stop();
        mCb.log("disabling " + mProvider);
        mCb.pass();
    }

    private void fail(String s) {
        stop();
        mCb.log("disabling");
        mCb.fail(s);
    }

    private void expectNextUpdate(long timeout) {
        mHandler.removeMessages(MSG_TIMEOUT);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_TIMEOUT), timeout);
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (!mRunning) return true;
        fail("timeout (" + mMaxInterval + "ms) waiting for " +
                mProvider + " location change");
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (!mRunning) return;

        mNumUpdates++;
        expectNextUpdate(mMaxInterval);

        long timestamp = SystemClock.elapsedRealtime();
        long delta = timestamp - mLastTimestamp;
        mLastTimestamp = timestamp;

        if (delta > mMaxInterval) {
            fail(mProvider + " location changed too slow: " + delta + "ms > " +
                    mMaxInterval + "ms");
            return;
        } else if (mNumUpdates == 1) {
            mCb.log("received " + mProvider + " location (1st update, " + delta + "ms)");
        } else if (delta < mMinInterval) {
            fail(mProvider + " location updated too fast: " + delta + "ms < " +
                    mMinInterval + "ms");
            return;
        } else {
            mCb.log("received " + mProvider + " location (" + delta + "ms)");
        }

        if (!mProvider.equals(location.getProvider())) {
            fail("wrong provider in callback, actual: " + location.getProvider() +
                    " expected: " + mProvider);
            return;
        }

        if (mNumUpdates >= mRequestedUpdates) {
            pass();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {  }

    @Override
    public void onProviderEnabled(String provider) {  }

    @Override
    public void onProviderDisabled(String provider) {   }
}