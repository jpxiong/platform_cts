/*
 * Copyright (C) 2010 The Android Open Source Project
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

/*
 * This file references fs_error.png, fs_good.png, fs_indeterminate.png,
 * and fs_warning.png which are licensed under Creative Commons 3.0
 * by fatcow.com.
 * http://www.fatcow.com/free-icons/
 * http://creativecommons.org/licenses/by/3.0/us/
 */
package com.android.cts.verifier.features;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.android.cts.verifier.R;

import android.app.ListActivity;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class FeatureSummaryActivity extends ListActivity {
    /**
     * Simple storage class for data about an Android feature.
     */
    static class Feature {
        /**
         * The name of the feature. Should be one of the PackageManager.FEATURE*
         * constants.
         */
        public String name;

        /**
         * Indicates whether the field is present on the current device.
         */
        public boolean present;

        /**
         * Indicates whether the field is required for the current device.
         */
        public boolean required;

        /**
         * Constructor does not include 'present' because that's a detected
         * value, and not set during creation.
         * 
         * @param name value for this.name
         * @param required value for this.required
         */
        public Feature(String name, boolean required) {
            this.name = name;
            this.required = required;
            this.present = false;
        }
    }

    /**
     * A list of all known features. If a constant is added to PackageManager,
     * this list needs to be updated. We could detect these fields via
     * Reflection, but we can't determine whether the features are required or
     * not that way, so we need this block anyway.
     */
    public static final Feature[] ALL_FEATURES = {
            new Feature(PackageManager.FEATURE_BLUETOOTH, true),
            new Feature(PackageManager.FEATURE_CAMERA, true),
            new Feature(PackageManager.FEATURE_CAMERA_AUTOFOCUS, false),
            new Feature(PackageManager.FEATURE_CAMERA_FLASH, false),
            new Feature(PackageManager.FEATURE_LIVE_WALLPAPER, false),
            new Feature(PackageManager.FEATURE_LOCATION, true),
            new Feature(PackageManager.FEATURE_LOCATION_GPS, true),
            new Feature(PackageManager.FEATURE_LOCATION_NETWORK, true),
            new Feature(PackageManager.FEATURE_MICROPHONE, true),
            new Feature(PackageManager.FEATURE_SENSOR_ACCELEROMETER, true),
            new Feature(PackageManager.FEATURE_SENSOR_COMPASS, true),
            new Feature(PackageManager.FEATURE_SENSOR_LIGHT, false),
            new Feature(PackageManager.FEATURE_SENSOR_PROXIMITY, false),
            new Feature(PackageManager.FEATURE_TELEPHONY, false),
            new Feature(PackageManager.FEATURE_TELEPHONY_CDMA, false),
            new Feature(PackageManager.FEATURE_TELEPHONY_GSM, false),
            new Feature(PackageManager.FEATURE_TOUCHSCREEN, true),
            new Feature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH, false),
            new Feature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT, false),
            new Feature(PackageManager.FEATURE_WIFI, false),
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fs_main);

        // some values used to detect warn-able conditions involving multiple features
        boolean hasWifi = false;
        boolean hasTelephony = false;
        boolean hasIllegalFeature = false;

        // get list of all features device thinks it has, & store in a HashMap for fast lookups
        HashMap<String, String> actualFeatures = new HashMap<String, String>();
        for (FeatureInfo fi : getPackageManager().getSystemAvailableFeatures()) {
            actualFeatures.put(fi.name, fi.name);
        }

        // data structure that the SimpleAdapter will use to populate ListView
        ArrayList<HashMap<String, Object>> listViewData = new ArrayList<HashMap<String, Object>>();

        // roll over all known features & check whether device reports them
        boolean present = false;
        int statusIcon;
        for (Feature f : ALL_FEATURES) {
            HashMap<String, Object> row = new HashMap<String, Object>();
            listViewData.add(row);
            present = actualFeatures.containsKey(f.name);
            if (present) {
                // device reports it -- yay! set the happy icon
                hasWifi = hasWifi || PackageManager.FEATURE_WIFI.equals(f.name);
                hasTelephony = hasTelephony || PackageManager.FEATURE_TELEPHONY.equals(f.name);
                statusIcon = R.drawable.fs_good;
                actualFeatures.remove(f.name);
            } else if (!present && f.required) {
                // it's required, but device doesn't report it. Boo, set the bogus icon
                statusIcon = R.drawable.fs_error;
            } else {
                // device doesn't report it, but it's not req'd, so can't tell if there's a problem
                statusIcon = R.drawable.fs_indeterminate;
            }
            row.put("feature", f.name);
            row.put("icon", statusIcon);
        }

        // now roll over any remaining features (which are non-standard)
        for (String feature : actualFeatures.keySet()) {
            if (feature == null || "".equals(feature))
                continue;
            HashMap<String, Object> row = new HashMap<String, Object>();
            listViewData.add(row);
            row.put("feature", feature);
            if (feature.startsWith("android")) { // intentionally not "android."
                // sorry, you're not allowed to squat in the official namespace; set bogus icon
                row.put("icon", R.drawable.fs_error);
                hasIllegalFeature = true;
            } else {
                // non-standard features are okay, but flag them just in case
                row.put("icon", R.drawable.fs_warning);
            }
        }

        // sort the ListView's data to group by icon type, for easier reading by humans
        final HashMap<Integer, Integer> idMap = new HashMap<Integer, Integer>();
        idMap.put(R.drawable.fs_error, 0);
        idMap.put(R.drawable.fs_warning, 1);
        idMap.put(R.drawable.fs_indeterminate, 2);
        idMap.put(R.drawable.fs_good, 3);
        Collections.sort(listViewData, new Comparator<HashMap<String, Object>>() {
            public int compare(HashMap<String, Object> left, HashMap<String, Object> right) {
                int leftId = idMap.get((Integer) (left.get("icon")));
                int rightId = idMap.get((Integer) (right.get("icon")));
                if (leftId == rightId) {
                    return ((String) left.get("feature")).compareTo((String) right.get("feature"));
                }
                if (leftId < rightId)
                    return -1;
                return 1;
            }
        });

        // Set up the SimpleAdapter used to populate the ListView
        SimpleAdapter adapter = new SimpleAdapter(this, listViewData, R.layout.fs_row,
            new String[] { "feature", "icon" },
            new int[] { R.id.fs_feature, R.id.fs_icon });
        adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            public boolean setViewValue(View view, Object data, String repr) {
                try {
                    if (view instanceof ImageView) {
                        ((ImageView) view).setImageResource((Integer) data);
                    } else if (view instanceof TextView) {
                        ((TextView) view).setText((String) data);
                    } else {
                        return false;
                    }
                    return true;
                } catch (ClassCastException e) {
                    return false;
                }
            }
        });
        setListAdapter(adapter);

        // finally, check for our second-order error cases and set warning text if necessary
        StringBuffer sb = new StringBuffer();
        if (hasIllegalFeature) {
            sb.append(getResources().getString(R.string.fs_disallowed)).append("\n");
        }
        if (!hasWifi && !hasTelephony) {
            sb.append(getResources().getString(R.string.fs_missing_wifi_telephony)).append("\n");
        }
        ((TextView) (findViewById(R.id.fs_warnings))).setText(sb.toString());
    }
}
