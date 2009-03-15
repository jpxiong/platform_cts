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

import java.util.ArrayList;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Device;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;

/**
 * Initializing and managing devices.
 */
public class DeviceManager implements IDeviceChangeListener {

    ArrayList<TestDevice> mDevices;

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
        int count =0;
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

    /** {@inheritDoc} */
    public void deviceChanged(Device device, int changeMask) {
        Log.d("device " + device.getSerialNumber() + " changed with changeMask=" + changeMask);
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

        public void run() {
            while (mDevice.getSyncService() == null || mDevice.getPropertyCount() == 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e("", e);
                }
            }
            CUIOutputStream.println("Device(" + mDevice + ") connected");
            CUIOutputStream.printPrompt();
            appendDevice(mDevice);
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
}
