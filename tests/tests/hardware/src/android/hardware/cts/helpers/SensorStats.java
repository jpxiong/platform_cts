/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.hardware.cts.helpers;

import android.hardware.cts.helpers.sensoroperations.ISensorOperation;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class used to store stats related to {@link ISensorOperation}s.  Sensor stats may be linked
 * together so that they form a tree.
 */
public class SensorStats {
    public static final String DELIMITER = "__";

    private final Map<String, Object> mValues = new HashMap<String, Object>();
    private final Map<String, SensorStats> mSensorStats = new HashMap<String, SensorStats>();

    /**
     * Add a value.
     *
     * @param key the key.
     * @param value the value as an {@link Object}.
     */
    public synchronized void addValue(String key, Object value) {
        if (value == null) {
            return;
        }
        mValues.put(key, value);
    }

    /**
     * Add a nested {@link SensorStats}. This is useful for keeping track of stats in a
     * {@link ISensorOperation} tree.
     *
     * @param key the key
     * @param stats the sub {@link SensorStats} object.
     */
    public synchronized void addSensorStats(String key, SensorStats stats) {
        if (stats == null) {
            return;
        }
        mSensorStats.put(key, stats);
    }

    /**
     * Flattens the map and all sub {@link SensorStats} objects. Keys will be flattened using
     * {@value #DELIMITER}. For example, if a sub {@link SensorStats} is added with key
     * {@code "key1"} containing the key value pair {@code ("key2", "value")}, the flattened map
     * will contain the entry {@code ("key1__key2", "value")}.
     *
     * @return a {@link Map} containing all stats from the value and sub {@link SensorStats}.
     */
    public synchronized Map<String, Object> flatten() {
        final Map<String, Object> flattenedMap = new HashMap<String, Object>(mValues);
        for (Entry<String, SensorStats> statsEntry : mSensorStats.entrySet()) {
            for (Entry<String, Object> valueEntry : statsEntry.getValue().flatten().entrySet()) {
                String key = statsEntry.getKey() + DELIMITER + valueEntry.getKey();
                flattenedMap.put(key, valueEntry.getValue());
            }
        }
        return flattenedMap;
    }

    /**
     * Utility method to log the stats to the logcat.
     */
    public static void logStats(String tag, SensorStats stats) {
        final Map<String, Object> flattened = stats.flatten();
        final List<String> keys = new ArrayList<String>(flattened.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            Object value = flattened.get(key);
            if (value instanceof Double || value instanceof Float) {
                Log.v(tag, String.format("%s: %.4f", key, value));
            } else {
                Log.v(tag, String.format("%s: %s", key, value.toString()));
            }
        }
    }
}
