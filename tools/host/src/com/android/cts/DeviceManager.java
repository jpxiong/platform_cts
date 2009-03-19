/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.cts;

import com.android.cts.TestDevice.StdOutObserver;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Device;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Initializing and managing devices.
 */
public class DeviceManager implements IDeviceChangeListener {

    private static final int SHORT_DELAY = 1000 * 15; // 15 seconds
    private static final int LONG_DELAY = 1000 * 60 * 10; // 10 minutes
    ArrayList<TestDevice> mDevices;
    /** This is used during device restart for blocking until the device has been reconnected. */
    private Semaphore mSemaphore = new Semaphore(0);
    
    public DeviceManager() {
        mDevices = new ArrayList<TestDevice>();
    }

    /**
     * Initialize Android debug bridge. This function should be called after
     * {@link DeviceManager} initialized.
     */
    public void initAdb() {
        String adbLocation = getAdbLocation();

        Log.d("init adb...");
        AndroidDebugBridge.init(true);
        AndroidDebugBridge.addDeviceChangeListener(this);
        AndroidDebugBridge.createBridge(adbLocation, true);
    }
    
    /**
     * Get the location of the adb command.
     *
     * @return The location of the adb location.
     */
    public static String getAdbLocation() {
        return "adb";
    }

    /**
     * Allocate devices by specified number for testing.
     * @param num the number of required device
     * @return the specified number of devices.
     */
    public TestDevice[] allocateDevices(final int num) throws DeviceNotAvailableException {

        ArrayList<TestDevice> deviceList;
        TestDevice td;
        int index = 0;

        if (num < 0) {
            throw new IllegalArgumentException();
        }
        if (num > mDevices.size()) {
            throw new DeviceNotAvailableException("The number of connected device("
                    + mDevices.size() + " is less than the specified number("
                    + num + "). Please plug in enough devices");
        }
        deviceList = new ArrayList<TestDevice>();

        while (index < mDevices.size() && deviceList.size() != num) {
            td = mDevices.get(index);
            if (td.getStatus() == TestDevice.STATUS_IDLE) {
                deviceList.add(td);
            }
            index++;
        }
        if (deviceList.size() != num) {
            throw new DeviceNotAvailableException("Can't get the specified number("
                    + num + ") of idle device(s).");
        }
        return deviceList.toArray(new TestDevice[num]);
    }

    /**
     * Get TestDevice list that available for executing tests.
     *
     * @return The device list.
     */
    public final TestDevice[] getDeviceList() {
        return mDevices.toArray(new TestDevice[mDevices.size()]);
    }

    /**
     * Get the number of all free devices.
     *
     * @return the number of all free devices
     */
    public int getCountOfFreeDevices() {
        int count = 0;
        for (TestDevice td : mDevices) {
            if (td.getStatus() == TestDevice.STATUS_IDLE) {
                count++;
            }
        }
        return count;
    }

    /**
     * Append the device to the device list.
     *
     * @param device The devie to be appended to the device list.
     */
    private void appendDevice(final Device device) {
        if (-1 == searchDevice(device)) {
            TestDevice td = new TestDevice(device);
            mDevices.add(td);
        }
    }

    /**
     * Remove specified TestDevice from managed list.
     *
     * @param device The device to be removed from the device list.
     */
    private void removeDevice(final Device device) {
        int index = searchDevice(device);
        if (index == -1) {
            Log.d("Can't find " + device + " in device list of DeviceManager");
            return;
        }
        mDevices.get(index).disconnected();
        mDevices.remove(index);
    }

    /**
     * Search a specific device.
     *
     * @param device The device to be found.
     * @return The index of the specific device if exits; else -1.
     */
    private int searchDevice(final Device device) {
        TestDevice td;

        for (int index = 0; index < mDevices.size(); index++) {
            td = mDevices.get(index);
            if (td.getSerialNumber().equals(device.getSerialNumber())) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Search a specific device.
     *
     * @param deviceSerialNumber The device to be found.
     * @return The the specific test device if exits; else null.
     */
    private TestDevice searchTestDevice(final String deviceSerialNumber) {
        for (TestDevice td : mDevices) {
            if (td.getSerialNumber().equals(deviceSerialNumber)) {
                return td;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    public void deviceChanged(Device device, int changeMask) {
        Log.d("device " + device.getSerialNumber() + " changed with changeMask=" + changeMask);
        Log.d("Device state:" + device.getState());
    }

    /** {@inheritDoc} */
    public void deviceConnected(Device device) {
        new DeviceServiceMonitor(device).start();
    }

    /**
     * To make sure that connection between {@link AndroidDebugBridge}
     * and {@link Device} is initialized properly. In fact, it just make sure
     * the sync service isn't null and device's build values are collected
     * before appending device.
     */
    private class DeviceServiceMonitor extends Thread {
        private Device mDevice;

        public DeviceServiceMonitor(Device device) {
            mDevice = device;
        }

        @Override
        public void run() {
            while (mDevice.getSyncService() == null || mDevice.getPropertyCount() == 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e("", e);
                }
            }
            CUIOutputStream.println("Device(" + mDevice + ") connected");
            if (!TestSession.isADBServerRestartedMode()) {
                CUIOutputStream.printPrompt();
            }
            appendDevice(mDevice);
            // increment the counter semaphore to unblock threads waiting for devices
            mSemaphore.release();
        }
    }

    /** {@inheritDoc} */
    public void deviceDisconnected(Device device) {
        removeDevice(device);
    }

    /**
     * Allocate device by specified Id for testing.
     * @param deviceId the ID of the test device.
     * @return a {@link TestDevice} if the specified device is free.
     */
    public TestDevice allocateFreeDeviceById(String deviceId) throws DeviceNotAvailableException {
        for (TestDevice td : mDevices) {
            if (td.getSerialNumber().equals(deviceId)) {
                if (td.getStatus() != TestDevice.STATUS_IDLE) {
                    String msg = "The specifed device(" + deviceId + ") is " +
                    td.getStatusAsString();
                    throw new DeviceNotAvailableException(msg);
                }
                return td;
            }
        }
        throw new DeviceNotAvailableException("The specified device(" +
                deviceId + "cannot be found");
    }

    /**
     * Reset the online {@link TestDevice} to STATUS_IDLE
     *
     * @param device of the specified {@link TestDevice}
     */
    public void resetTestDevice(final TestDevice device) {
        if (device.getStatus() != TestDevice.STATUS_OFFLINE) {
            device.setStatus(TestDevice.STATUS_IDLE);
        }
    }

    /**
     * Restart ADB server.
     *
     * @param ts The test session.
     */
    public void restartADBServer(TestSession ts) throws DeviceDisconnectedException {
        try {
            Thread.sleep(SHORT_DELAY); // time to collect outstanding logs
            Log.i("Restarting device ...");
            rebootDevice(ts);
            Log.i("Restart complete.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reboot the device.
     *
     * @param ts The test session.
     */
    private void rebootDevice(TestSession ts) throws InterruptedException,
                DeviceDisconnectedException {

        String deviceSerialNumber = ts.getDeviceId();
        if (!deviceSerialNumber.toLowerCase().startsWith("emulator")) {
            AndroidDebugBridge.disconnectBridge();
            executeCommand("adb shell reboot");
            // kill the server while the device is rebooting
            executeCommand("adb kill-server");
            
            // Reset the device counter semaphore. We will wait below until at least one device
            // has come online. This can happen any time during or after the call to
            // createBridge(). The counter gets increased by the DeviceServiceMonitor when a
            // device is added.
            mSemaphore.drainPermits();
            AndroidDebugBridge.createBridge(getAdbLocation(), true);
            
            // wait until at least one device has been added
            mSemaphore.tryAcquire(LONG_DELAY, TimeUnit.MILLISECONDS);

            // wait until the device has started up
            // TODO: Can we use 'adb shell getprop dev.bootcomplete' instead?
            boolean started = false;
            while (!started) {
                Thread.sleep(SHORT_DELAY);
                // dump log and exit
                RestartADBServerObserver ro = executeCommand("adb logcat -d");
                started = ro.hasStarted();
            }

            TestDevice device = searchTestDevice(deviceSerialNumber);
            if (device != null) {
                ts.setTestDevice(device);
                ts.getSessionLog().setDeviceInfo(device.getDeviceInfo());
                Thread.sleep(SHORT_DELAY);
                device.probeDeviceStatus();
                Thread.sleep(SHORT_DELAY * 2);
            }
            
            // dismiss the screen lock by sending a MENU key event
            executeCommand("adb shell input keyevent 82");
        }
    }
    
    /**
     * Execute the given command and wait for its completion.
     *
     * @param command The command to be executed.
     */
    private RestartADBServerObserver executeCommand(String command) {
        Log.d("restartADBServer(): cmd=" + command);
        RestartADBServerObserver ro = new RestartADBServerObserver();
        try {
            Process proc = Runtime.getRuntime().exec(command);
            ro.setInputStream(proc.getInputStream());
            ro.waitToFinish();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ro;
    }


    /**
     * The observer of restarting ADB server.
     * TODO: This is a workaround to reset the device after a certain number of tests have been run.
     * This should be removed as soon as the ADB stability problems have been fixed.
     */
    final class RestartADBServerObserver implements StdOutObserver, Runnable {

        private BufferedReader mReader;
        private final Pattern mPattern = 
            Pattern.compile("I/SurfaceFlinger.*Boot is finished.*");
        private boolean mFoundPattern;
        private Thread mThread;
        private boolean mKillThread;
        
        /**
         * Wait for the observer to process all lines.
         * @return True if the observer finished normally, false if a timeout occurred.
         */
        public boolean waitToFinish() {
            // wait for thread to terminate first
            try {
                mThread.join(LONG_DELAY);
                // set the kill flag, just in case we timed out
                mKillThread = true;
                return true;
            } catch (InterruptedException e) {
                mKillThread = true;
                return false;
            }
        }
        
        public boolean hasStarted() {
            waitToFinish();
            return mFoundPattern;
        }
        
        /** {@inheritDoc} */
        public void run() {
            try {
                processLines();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    mReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Parse the standard out.
         */
        public void processLines() throws IOException {
            String line = mReader.readLine();
            
            while (line != null && !mKillThread) {
                Log.d("line=" + line);
                if (!mFoundPattern) {
                    mFoundPattern = mPattern.matcher(line.trim()).matches();
                }
                line = mReader.readLine();
            }
        }

        /** {@inheritDoc} */
        public void setInputStream(InputStream is) {
            mReader = new BufferedReader(new InputStreamReader(is));
            mThread = new Thread(this);
            mThread.start();
        }
    }
}
