/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.bluetooth.cts;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.SystemClock;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Test cases for Bluetooth LE scans.
 * <p>
 * To run the test, the device must be placed in an environment that has at least 5 BLE beacons
 * broadcasting no slower than 1 HZ. The BLE beacons should be a combination of iBeacon devices and
 * non-iBeacon devices.
 * <p>
 * Run 'run cts --class android.bluetooth.cts.BluetoothLeScanTest' in cts-tradefed to run the test
 * cases.
 */
public class BluetoothLeScanTest extends AndroidTestCase {

    private static final String TAG = "BluetoothLeScanTest";

    private static final ScanFilter MANUFACTURER_DATA_FILTER =
            new ScanFilter.Builder().setManufacturerData(0x004C, new byte[0], new byte[0]).build();
    private static final int SCAN_DURATION_MILLIS = 5000;
    private static final int BATCH_SCAN_REPORT_DELAY_MILLIS = 10000;

    private BluetoothLeScanner mScanner;

    @Override
    public void setUp() {
        BluetoothManager manager = (BluetoothManager) mContext.getSystemService(
                Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
        if (!adapter.isEnabled()) {
            adapter.enable();
            // Note it's not reliable to listen for Adapter.ACTION_STATE_CHANGED broadcast and check
            // bluetooth state.
            sleep(2000);
        }
        mScanner = adapter.getBluetoothLeScanner();
    }

    /**
     * Basic test case for BLE scans. Checks BLE scan timestamp is within correct range.
     */
    @MediumTest
    public void testBasicBleScan() {
        BleScanCallback regularLeScanCallback = new BleScanCallback();
        long scanStartMillis = SystemClock.elapsedRealtime();
        mScanner.startScan(regularLeScanCallback);
        sleep(SCAN_DURATION_MILLIS);
        mScanner.stopScan(regularLeScanCallback);
        long scanEndMillis = SystemClock.elapsedRealtime();
        Collection<ScanResult> scanResults = regularLeScanCallback.getScanResults();
        assertTrue("Scan results shouldn't be empty", !scanResults.isEmpty());
        verifyTimestamp(scanResults, scanStartMillis, scanEndMillis);
    }

    /**
     * Test of scan filters. Ensures only beacons matching certain type of scan filters were
     * reported.
     */
    @MediumTest
    public void testScanFilter() {
        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        filters.add(MANUFACTURER_DATA_FILTER);

        BleScanCallback filterLeScanCallback = new BleScanCallback();
        ScanSettings settings = new ScanSettings.Builder().setScanMode(
                ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        mScanner.startScan(filters, settings, filterLeScanCallback);
        sleep(SCAN_DURATION_MILLIS);
        mScanner.stopScan(filterLeScanCallback);
        Collection<ScanResult> scanResults = filterLeScanCallback.getScanResults();
        assertTrue("No scan results", !scanResults.isEmpty());
        for (ScanResult result : scanResults) {
            assertTrue(MANUFACTURER_DATA_FILTER.matches(result));
        }
    }

    /**
     * Test of opportunistic BLE scans.
     */
    @MediumTest
    public void testOpportunisticScan() {
        ScanSettings opportunisticScanSettings =
                new ScanSettings.Builder()
                        .setScanMode(-1) // TODO: use constants in ScanSettings once it's unhiden.
                        .build();
        BleScanCallback emptyScanCallback = new BleScanCallback();

        // No scans are really started with opportunistic scans only.
        mScanner.startScan(Collections.<ScanFilter> emptyList(), opportunisticScanSettings,
                emptyScanCallback);
        sleep(SCAN_DURATION_MILLIS);
        assertTrue(emptyScanCallback.getScanResults().isEmpty());

        BleScanCallback regularScanCallback = new BleScanCallback();
        ScanSettings regularScanSettings =
                new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(MANUFACTURER_DATA_FILTER);
        mScanner.startScan(filters, regularScanSettings, regularScanCallback);
        sleep(SCAN_DURATION_MILLIS);
        // With normal BLE scan client, opportunistic scan client will get scan results.
        assertTrue("opportunistic scan results shouldn't be empty",
                !emptyScanCallback.getScanResults().isEmpty());
        assertTrue("opportunistic scan should see more results",
                emptyScanCallback.getScanResults().size() >
                regularScanCallback.getScanResults().size());

        // No more scan results for opportunistic scan clients once the normal BLE scan clients
        // stops.
        mScanner.stopScan(regularScanCallback);
        // In case we got scan results before scan was completely stopped.
        sleep(1000);
        emptyScanCallback.clear();
        sleep(SCAN_DURATION_MILLIS);
        assertTrue("opportunistic scan shouldn't have scan results",
                emptyScanCallback.getScanResults().isEmpty());
    }

    /**
     * Test case for BLE Batch scan.
     */
    @MediumTest
    public void testBatchScan() {
        ScanSettings batchScanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(BATCH_SCAN_REPORT_DELAY_MILLIS).build();
        BleScanCallback batchScanCallback = new BleScanCallback();
        long scanStartMillis = SystemClock.elapsedRealtime();
        mScanner.startScan(Collections.<ScanFilter> emptyList(), batchScanSettings,
                batchScanCallback);
        sleep(SCAN_DURATION_MILLIS);
        mScanner.flushPendingScanResults(batchScanCallback);
        sleep(1000);
        long scanEndMillis = SystemClock.elapsedRealtime();
        List<ScanResult> results = batchScanCallback.getBatchScanResults();
        assertTrue(!results.isEmpty());
        verifyTimestamp(results, scanStartMillis, scanEndMillis);
    }

    // Verify timestamp of all scan results are within [scanStartMillis, scanEndMillis].
    private void verifyTimestamp(Collection<ScanResult> results, long scanStartMillis,
            long scanEndMillis) {
        for (ScanResult result : results) {
            long timestampMillis = TimeUnit.NANOSECONDS.toMillis(result.getTimestampNanos());
            assertTrue("Invalid timestamp", timestampMillis >= scanStartMillis);
            assertTrue("Invalid timestamp", timestampMillis <= scanEndMillis);
        }
    }

    // Helper class for BLE scan callback.
    private class BleScanCallback extends ScanCallback {
        private Set<ScanResult> mResults = new HashSet<ScanResult>();
        private List<ScanResult> mBatchScanResults = new ArrayList<ScanResult>();

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                mResults.add(result);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            mBatchScanResults = results;
        }

        // Clear regular and batch scan results.
        synchronized public void clear() {
            mResults.clear();
            mBatchScanResults.clear();
        }

        // Return regular BLE scan results accumulated so far.
        synchronized Collection<ScanResult> getScanResults() {
            return Collections.unmodifiableCollection(mResults);
        }

        // Return batch scan results.
        synchronized List<ScanResult> getBatchScanResults() {
            return Collections.unmodifiableList(mBatchScanResults);
        }
    }

    // Put the current thread to sleep.
    private void sleep(int sleepMillis) {
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            Log.e(TAG, "interrupted", e);
        }
    }

}
