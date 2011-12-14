/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.security.cts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.test.AndroidTestCase;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;

public class PIILogTest extends AndroidTestCase {

    private boolean mHasLocation;
    private boolean mIsPhone;
    private LocationManager mLocationManager;
    private PackageManager mPackageManager;
    private String mPhoneNumber;
    private TelephonyManager mTelephonyManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mPackageManager = mContext.getPackageManager();

        mIsPhone = mPackageManager.hasSystemFeature(
                PackageManager.FEATURE_TELEPHONY);
        if (mIsPhone) {
            mTelephonyManager = (TelephonyManager)mContext.getSystemService(
                    Context.TELEPHONY_SERVICE);
            mPhoneNumber = mTelephonyManager.getLine1Number();
        }

        mHasLocation = mPackageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION);
        if (mHasLocation) {
            mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private Location getLocation() {
        return mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    }

    public void testLogEmail() throws Exception {
        // get the account manager
        AccountManager accountManager = AccountManager.get(mContext);

        // get all the accounts
        Account[] accounts = accountManager.getAccounts();

        // iterate over the accounts
        for (Account accnt : accounts) {

            // get the portion of the account prior to an '@', if present
            String fullName = accnt.name;
            String nameFragment = fullName;
            int index = fullName.indexOf("@");
            if (index > 0) {
                nameFragment = fullName.substring(0, index);
            }

            // check to make sure this account doesn't show up in the logs
            assertValueNotInLogs(nameFragment);
        }
    }

    public void testLogPhoneCalls() throws Exception {
        // check to make sure we're a phone...
        if (mIsPhone && (mPhoneNumber != null)) {
            assertValueNotInLogs(mPhoneNumber);
        }
    }

    public void testLogIMEI() throws Exception {
        // check to make sure we're a phone...
        if (mIsPhone) {

            // get our imei
            String imei = mTelephonyManager.getDeviceId();

            // check if the number shows up in logs
            assertValueNotInLogs(imei);
        }
    }

    public void testLogLocation() throws Exception {
        // check to make sure we can get location data
        if (mHasLocation) {

            // try to get our location
            Location location = getLocation();

            // we didn't get it, and so need to bail.
            // this is done to avoid a situation in which the test fails due
            // to network conditions or a timeout.
            if (location == null) {
                return;
            }

            // get our latitude and longitude
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();

            // check if either of those show up in the logs
            assertValueNotInLogs(Double.toString(longitude));

            assertValueNotInLogs(Double.toString(latitude));

            // get other names for where we are
            Geocoder geo = new Geocoder(mContext);
            List<Address> addresses = geo.getFromLocation(latitude, longitude, 10);

            // iterate over those
            for (Address addr : addresses) {
                // we want to check for several interesting bits of data:
                //    1. look for the locality
                //    2. look for the sub-locality
                //    3. look for the thoroughfare
                //    4. look for the postal code
                String locality = addr.getLocality();
                if (locality != null) {
                    assertValueNotInLogs(locality);
                }

                String sublocality = addr.getSubLocality();
                if (sublocality != null) {
                    assertValueNotInLogs(sublocality);
                }

                String thoroughfare = addr.getThoroughfare();
                if (thoroughfare != null) {
                    assertValueNotInLogs(thoroughfare);
                }

                String postal_code = addr.getPostalCode();
                if (postal_code != null) {
                    assertValueNotInLogs(postal_code);
                }
            }
        }
    }

    public void testLogSMS() throws Exception {
        // check to make sure we're a phone...
        if (mIsPhone && (mPhoneNumber != null)) {

            // send an SMS to our number
            SmsManager manager = SmsManager.getDefault();
            manager.sendTextMessage(mPhoneNumber, null, "%^&*(", null, null);

            // check if that number shows up in logs
            assertValueNotInLogs(mPhoneNumber);

            // check if any message fragments show up in logs
            assertValueNotInLogs("%^&*(");
        }
    }

    private void assertValueNotInLogs(String value) throws Exception {
        StringBuilder failLines = new StringBuilder();
        Process process = Runtime.getRuntime().exec("logcat -d *:IWFSE");
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.contains(value)) {
                failLines.append(line);
            }
        }
        bufferedReader.close();
        String message = failLines.toString();
        assertTrue(message, message.isEmpty()); 
    }
}
