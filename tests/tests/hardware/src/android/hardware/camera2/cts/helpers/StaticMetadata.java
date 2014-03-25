/*
 * Copyright 2014 The Android Open Source Project
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

package android.hardware.camera2.cts.helpers;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CameraMetadata.Key;
import android.util.Log;

import junit.framework.Assert;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Helpers to get common static info out of the camera.
 *
 * <p>Avoid boiler plate by putting repetitive get/set patterns in this class.</p>
 *
 * <p>Attempt to be durable against the camera device having bad or missing metadata
 * by providing reasonable defaults and logging warnings when that happens.</p>
 */
public class StaticMetadata {

    private static final String TAG = "StaticMetadata";
    private static final int IGNORE_SIZE_CHECK = -1;

    // TODO: don't hardcode, generate from metadata XML
    private static final int SENSOR_INFO_EXPOSURE_TIME_RANGE_SIZE = 2;
    private static final int SENSOR_INFO_EXPOSURE_TIME_RANGE_MIN = 0;
    private static final int SENSOR_INFO_EXPOSURE_TIME_RANGE_MAX = 1;
    private static final long SENSOR_INFO_EXPOSURE_TIME_RANGE_MIN_AT_MOST = 100000L; // 100us
    private static final long SENSOR_INFO_EXPOSURE_TIME_RANGE_MAX_AT_LEAST = 100000000; // 100ms
    private static final int SENSOR_INFO_SENSITIVITY_RANGE_SIZE = 2;
    private static final int SENSOR_INFO_SENSITIVITY_RANGE_MIN = 0;
    private static final int SENSOR_INFO_SENSITIVITY_RANGE_MAX = 1;
    private static final int SENSOR_INFO_SENSITIVITY_RANGE_MIN_AT_MOST = 100;
    private static final int SENSOR_INFO_SENSITIVITY_RANGE_MAX_AT_LEAST = 1600;

    // TODO: Consider making this work across any metadata object, not just camera characteristics
    private final CameraCharacteristics mCharacteristics;
    private final CheckLevel mLevel;
    private final CameraErrorCollector mCollector;

    public enum CheckLevel {
        /** Only log warnings for metadata check failures. Execution continues. */
        WARN,
        /**
         * Use ErrorCollector to collect the metadata check failures, Execution
         * continues.
         */
        COLLECT,
        /** Assert the metadata check failures. Execution aborts. */
        ASSERT
    }

    /**
     * Construct a new StaticMetadata object.
     *
     *<p> Default constructor, only log warnings for the static metadata check failures</p>
     *
     * @param characteristics static info for a camera
     * @throws IllegalArgumentException if characteristics was null
     */
    public StaticMetadata(CameraCharacteristics characteristics) {
        this(characteristics, CheckLevel.WARN, /*collector*/null);
    }

    /**
     * Construct a new StaticMetadata object with {@link CameraErrorCollector}.
     * <p>
     * When level is not {@link CheckLevel.COLLECT}, the {@link CameraErrorCollector} will be
     * ignored, otherwise, it will be used to log the check failures.
     * </p>
     *
     * @param characteristics static info for a camera
     * @param collector The {@link CameraErrorCollector} used by this StaticMetadata
     * @throws IllegalArgumentException if characteristics or collector was null.
     */
    public StaticMetadata(CameraCharacteristics characteristics, CameraErrorCollector collector) {
        this(characteristics, CheckLevel.COLLECT, collector);
    }

    /**
     * Construct a new StaticMetadata object with {@link CheckLevel} and
     * {@link CameraErrorCollector}.
     * <p>
     * When level is not {@link CheckLevel.COLLECT}, the {@link CameraErrorCollector} will be
     * ignored, otherwise, it will be used to log the check failures.
     * </p>
     *
     * @param characteristics static info for a camera
     * @param level The {@link CheckLevel} of this StaticMetadata
     * @param collector The {@link CameraErrorCollector} used by this StaticMetadata
     * @throws IllegalArgumentException if characteristics was null or level was
     *         {@link CheckLevel.COLLECT} but collector was null.
     */
    public StaticMetadata(CameraCharacteristics characteristics, CheckLevel level,
            CameraErrorCollector collector) {
        if (characteristics == null) {
            throw new IllegalArgumentException("characteristics was null");
        }
        if (level == CheckLevel.COLLECT && collector == null) {
            throw new IllegalArgumentException("collector must valid when COLLECT level is set");
        }

        mCharacteristics = characteristics;
        mLevel = level;
        mCollector = collector;
    }

    /**
     * Get the CameraCharacteristics associated with this StaticMetadata.
     *
     * @return A non-null CameraCharacteristics object
     */
    public CameraCharacteristics getCharacteristics() {
        return mCharacteristics;
    }

    /**
     * Whether or not the hardware level reported by android.info.supportedHardwareLevel
     * is {@value CameraMetadata#INFO_SUPPORTED_HARDWARE_LEVEL_FULL}.
     *
     * <p>If the camera device is incorrectly reporting the hardwareLevel, this
     * will always return {@code false}.</p>
     *
     * @return true if the device is FULL, false otherwise.
     */
    public boolean isHardwareLevelFull() {
        // TODO: Make this key non-optional for all HAL3.2+ devices
        Integer hwLevel = getValueFromKeyNonNull(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

        // Bad. Missing metadata. Warning is logged.
        if (hwLevel == null) {
            return false;
        }

        // Normal. Device could be limited.
        int hwLevelInt = hwLevel;
        return hwLevelInt == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL;
    }

    /**
     * Whether or not the hardware level reported by android.info.supportedHardwareLevel
     * is {@value CameraMetadata#INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED}.
     *
     * <p>If the camera device is incorrectly reporting the hardwareLevel, this
     * will always return {@code true}.</p>
     *
     * @return true if the device is LIMITED, false otherwise.
     */
    public boolean isHardwareLevelLimited() {
        return !isHardwareLevelFull();
    }

    /**
     * Get the exposure time value and clamp to the range if needed.
     *
     * @param exposure Input exposure time value to check.
     * @return Exposure value in the legal range.
     */
    public long getExposureClampToRange(long exposure) {
        long minExposure = getExposureMinimumOrDefault(Long.MAX_VALUE);
        long maxExposure = getExposureMaximumOrDefault(Long.MIN_VALUE);
        if (minExposure > SENSOR_INFO_EXPOSURE_TIME_RANGE_MIN_AT_MOST) {
            failKeyCheck(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE,
                    String.format(
                    "Min value %d is too large, set to maximal legal value %d",
                    minExposure, SENSOR_INFO_EXPOSURE_TIME_RANGE_MIN_AT_MOST));
            minExposure = SENSOR_INFO_EXPOSURE_TIME_RANGE_MIN_AT_MOST;
        }
        if (maxExposure < SENSOR_INFO_EXPOSURE_TIME_RANGE_MAX_AT_LEAST) {
            failKeyCheck(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE,
                    String.format(
                    "Max value %d is too small, set to minimal legal value %d",
                    maxExposure, SENSOR_INFO_EXPOSURE_TIME_RANGE_MAX_AT_LEAST));
            maxExposure = SENSOR_INFO_EXPOSURE_TIME_RANGE_MAX_AT_LEAST;
        }

        return Math.max(minExposure, Math.min(maxExposure, exposure));
    }

    /**
     * Get the available anti-banding modes.
     *
     * @return The array contains available anti-banding modes.
     */
    public byte[] getAeAvailableAntiBandingModesChecked() {
        CameraMetadata.Key<byte[]> key =
                CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES;
        byte[] modes = getValueFromKeyNonNull(key);

        boolean foundAuto = false;
        for (byte mode : modes) {
            checkTrueForKey(key, "mode value " + mode + " is out if range",
                    mode >= CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_OFF ||
                    mode <= CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO);
            if (mode == CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO) {
                foundAuto = true;
                return modes;
            }
        }
        // Must contain AUTO mode.
        checkTrueForKey(key, "AUTO mode is missing", foundAuto);

        return modes;
    }

    public Boolean getFlashInfoChecked() {
        CameraMetadata.Key<Boolean> key = CameraCharacteristics.FLASH_INFO_AVAILABLE;
        Boolean hasFlash = getValueFromKeyNonNull(key);

        // In case the failOnKey only gives warning.
        if (hasFlash == null) {
            return false;
        }

        return hasFlash;
    }

    /**
     * Get the sensitivity value and clamp to the range if needed.
     *
     * @param sensitivity Input sensitivity value to check.
     * @return Sensitivity value in legal range.
     */
    public int getSensitivityClampToRange(int sensitivity) {
        int minSensitivity = getSensitivityMinimumOrDefault(Integer.MAX_VALUE);
        int maxSensitivity = getSensitivityMaximumOrDefault(Integer.MIN_VALUE);
        if (minSensitivity > SENSOR_INFO_SENSITIVITY_RANGE_MIN_AT_MOST) {
            failKeyCheck(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE,
                    String.format(
                    "Min value %d is too large, set to maximal legal value %d",
                    minSensitivity, SENSOR_INFO_SENSITIVITY_RANGE_MIN_AT_MOST));
            minSensitivity = SENSOR_INFO_SENSITIVITY_RANGE_MIN_AT_MOST;
        }
        if (maxSensitivity < SENSOR_INFO_SENSITIVITY_RANGE_MAX_AT_LEAST) {
            failKeyCheck(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE,
                    String.format(
                    "Max value %d is too small, set to minimal legal value %d",
                    maxSensitivity, SENSOR_INFO_SENSITIVITY_RANGE_MAX_AT_LEAST));
            maxSensitivity = SENSOR_INFO_SENSITIVITY_RANGE_MAX_AT_LEAST;
        }

        return Math.max(minSensitivity, Math.min(maxSensitivity, sensitivity));
    }

    /**
     * Get the minimum value for a sensitivity range from android.sensor.info.sensitivityRange.
     *
     * <p>If the camera is incorrectly reporting values, log a warning and return
     * the default value instead, which is the largest minimum value required to be supported
     * by all camera devices.</p>
     *
     * @return The value reported by the camera device or the defaultValue otherwise.
     */
    public int getSensitivityMinimumOrDefault() {
        return getSensitivityMinimumOrDefault(SENSOR_INFO_SENSITIVITY_RANGE_MIN_AT_MOST);
    }

    /**
     * Get the minimum value for a sensitivity range from android.sensor.info.sensitivityRange.
     *
     * <p>If the camera is incorrectly reporting values, log a warning and return
     * the default value instead.</p>
     *
     * @param defaultValue Value to return if no legal value is available
     * @return The value reported by the camera device or the defaultValue otherwise.
     */
    public int getSensitivityMinimumOrDefault(int defaultValue) {
        return getArrayElementOrDefault(
                CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE,
                defaultValue,
                "minimum",
                SENSOR_INFO_SENSITIVITY_RANGE_MIN,
                SENSOR_INFO_SENSITIVITY_RANGE_SIZE);
    }

    /**
     * Get the maximum value for a sensitivity range from android.sensor.info.sensitivityRange.
     *
     * <p>If the camera is incorrectly reporting values, log a warning and return
     * the default value instead, which is the smallest maximum value required to be supported
     * by all camera devices.</p>
     *
     * @return The value reported by the camera device or the defaultValue otherwise.
     */
    public int getSensitivityMaximumOrDefault() {
        return getSensitivityMaximumOrDefault(SENSOR_INFO_SENSITIVITY_RANGE_MAX_AT_LEAST);
    }

    /**
     * Get the maximum value for a sensitivity range from android.sensor.info.sensitivityRange.
     *
     * <p>If the camera is incorrectly reporting values, log a warning and return
     * the default value instead.</p>
     *
     * @param defaultValue Value to return if no legal value is available
     * @return The value reported by the camera device or the defaultValue otherwise.
     */
    public int getSensitivityMaximumOrDefault(int defaultValue) {
        return getArrayElementOrDefault(
                CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE,
                defaultValue,
                "maximum",
                SENSOR_INFO_SENSITIVITY_RANGE_MAX,
                SENSOR_INFO_SENSITIVITY_RANGE_SIZE);
    }

    /**
     * Get the minimum value for an exposure range from android.sensor.info.exposureTimeRange.
     *
     * <p>If the camera is incorrectly reporting values, log a warning and return
     * the default value instead.</p>
     *
     * @param defaultValue Value to return if no legal value is available
     * @return The value reported by the camera device or the defaultValue otherwise.
     */
    public long getExposureMinimumOrDefault(long defaultValue) {
        return getArrayElementOrDefault(
                CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE,
                defaultValue,
                "minimum",
                SENSOR_INFO_EXPOSURE_TIME_RANGE_MIN,
                SENSOR_INFO_EXPOSURE_TIME_RANGE_SIZE);
    }

    /**
     * Get the minimum value for an exposure range from android.sensor.info.exposureTimeRange.
     *
     * <p>If the camera is incorrectly reporting values, log a warning and return
     * the default value instead, which is the largest minimum value required to be supported
     * by all camera devices.</p>
     *
     * @return The value reported by the camera device or the defaultValue otherwise.
     */
    public long getExposureMinimumOrDefault() {
        return getExposureMinimumOrDefault(SENSOR_INFO_EXPOSURE_TIME_RANGE_MIN_AT_MOST);
    }

    /**
     * Get the maximum value for an exposure range from android.sensor.info.exposureTimeRange.
     *
     * <p>If the camera is incorrectly reporting values, log a warning and return
     * the default value instead.</p>
     *
     * @param defaultValue Value to return if no legal value is available
     * @return The value reported by the camera device or the defaultValue otherwise.
     */
    public long getExposureMaximumOrDefault(long defaultValue) {
        return getArrayElementOrDefault(
                CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE,
                defaultValue,
                "maximum",
                SENSOR_INFO_EXPOSURE_TIME_RANGE_MAX,
                SENSOR_INFO_EXPOSURE_TIME_RANGE_SIZE);
    }

    /**
     * Get the maximum value for an exposure range from android.sensor.info.exposureTimeRange.
     *
     * <p>If the camera is incorrectly reporting values, log a warning and return
     * the default value instead, which is the smallest maximum value required to be supported
     * by all camera devices.</p>
     *
     * @return The value reported by the camera device or the defaultValue otherwise.
     */
    public long getExposureMaximumOrDefault() {
        return getExposureMaximumOrDefault(SENSOR_INFO_EXPOSURE_TIME_RANGE_MAX_AT_LEAST);
    }

    /**
     * Get aeAvailableModes and do the sanity check.
     *
     * <p>Depending on the check level this class has, for WAR or COLLECT levels,
     * If the aeMode list is invalid, return an empty mode array. The the caller doesn't
     * have to abort the execution even the aeMode list is invalid.</p>
     * @return AE available modes
     */
    public byte[] getAeAvailableModesChecked() {
        CameraMetadata.Key<byte[]> modesKey = CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES;
        byte[] modes = getValueFromKeyNonNull(modesKey);
        if (modes == null) {
            modes = new byte[0];
        }
        List<Integer> modeList = new ArrayList<Integer>();
        for (byte mode : modes) {
            modeList.add((int)(mode));
        }
        checkTrueForKey(modesKey, "value is empty", !modeList.isEmpty());

        // All camera device must support ON
        checkTrueForKey(modesKey, "values " + modeList.toString() + " must contain ON mode",
                modeList.contains(CameraMetadata.CONTROL_AE_MODE_ON));

        // All camera devices with flash units support ON_AUTO_FLASH and ON_ALWAYS_FLASH
        CameraMetadata.Key<Boolean> flashKey= CameraCharacteristics.FLASH_INFO_AVAILABLE;
        Boolean hasFlash = getValueFromKeyNonNull(flashKey);
        if (hasFlash == null) {
            hasFlash = false;
        }
        if (hasFlash) {
            boolean flashModeConsistentWithFlash =
                    modeList.contains(CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH) &&
                    modeList.contains(CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
            checkTrueForKey(modesKey,
                    "value must contain ON_AUTO_FLASH and ON_ALWAYS_FLASH and  when flash is" +
                    "available", flashModeConsistentWithFlash);
        } else {
            boolean flashModeConsistentWithoutFlash =
                    !(modeList.contains(CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH) ||
                    modeList.contains(CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH) ||
                    modeList.contains(CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE));
            checkTrueForKey(modesKey,
                    "value must not contain ON_AUTO_FLASH, ON_ALWAYS_FLASH and" +
                    "ON_AUTO_FLASH_REDEYE when flash is unavailable",
                    flashModeConsistentWithoutFlash);
        }

        // FULL mode camera devices always support OFF mode.
        boolean condition =
                !isHardwareLevelFull() || modeList.contains(CameraMetadata.CONTROL_AE_MODE_OFF);
        checkTrueForKey(modesKey, "Full capability device must have OFF mode", condition);

        // Boundary check.
        for (byte mode : modes) {
            checkTrueForKey(modesKey, "Value " + mode + " is out of bound",
                    mode >= CameraMetadata.CONTROL_AE_MODE_OFF
                    && mode <= CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE);
        }

        return modes;
    }

    /**
     * Get the value in index for a fixed-size array from a given key.
     *
     * <p>If the camera device is incorrectly reporting values, log a warning and return
     * the default value instead.</p>
     *
     * @param key Key to fetch
     * @param defaultValue Default value to return if camera device uses invalid values
     * @param name Human-readable name for the array index (logging only)
     * @param index Array index of the subelement
     * @param size Expected fixed size of the array
     *
     * @return The value reported by the camera device, or the defaultValue otherwise.
     */
    private <T> T getArrayElementOrDefault(Key<?> key, T defaultValue, String name, int index,
            int size) {
        T elementValue = getArrayElementCheckRangeNonNull(
                key,
                index,
                size);

        if (elementValue == null) {
            failKeyCheck(key,
                    "had no valid " + name + " value; using default of " + defaultValue);
            elementValue = defaultValue;
        }

        return elementValue;
    }

    /**
     * Fetch an array sub-element from an array value given by a key.
     *
     * <p>
     * Prints a warning if the sub-element was null.
     * </p>
     *
     * <p>Use for variable-size arrays since this does not check the array size.</p>
     *
     * @param key Metadata key to look up
     * @param element A non-negative index value.
     * @return The array sub-element, or null if the checking failed.
     */
    private <T> T getArrayElementNonNull(Key<?> key, int element) {
        return getArrayElementCheckRangeNonNull(key, element, IGNORE_SIZE_CHECK);
    }

    /**
     * Fetch an array sub-element from an array value given by a key.
     *
     * <p>
     * Prints a warning if the array size does not match the size, or if the sub-element was null.
     * </p>
     *
     * @param key Metadata key to look up
     * @param element The index in [0,size)
     * @param size A positive size value or otherwise {@value #IGNORE_SIZE_CHECK}
     * @return The array sub-element, or null if the checking failed.
     */
    private <T> T getArrayElementCheckRangeNonNull(Key<?> key, int element, int size) {
        Object array = getValueFromKeyNonNull(key);

        if (array == null) {
            // Warning already printed
            return null;
        }

        if (size != IGNORE_SIZE_CHECK) {
            int actualLength = Array.getLength(array);
            if (actualLength != size) {
                failKeyCheck(key,
                        String.format("had the wrong number of elements (%d), expected (%d)",
                                actualLength, size));
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        T val = (T) Array.get(array, element);

        if (val == null) {
            failKeyCheck(key, "had a null element at index" + element);
            return null;
        }

        return val;
    }

    /**
     * Gets the key, logging warnings for null values.
     */
    private <T> T getValueFromKeyNonNull(Key<T> key) {
        if (key == null) {
            throw new IllegalArgumentException("key was null");
        }

        T value = mCharacteristics.get(key);

        if (value == null) {
            failKeyCheck(key, "was null");
        }

        return value;
    }

    private <T> void checkTrueForKey(Key<T> key, String message, boolean condition) {
        if (!condition) {
            failKeyCheck(key, message);
        }
    }

    private <T> void failKeyCheck(Key<T> key, String message) {
        // TODO: Consider only warning once per key/message combination if it's too spammy.
        // TODO: Consider offering other options such as throwing an assertion exception
        String failureCause = String.format("The static info key '%s' %s", key.getName(), message);
        switch (mLevel) {
            case WARN:
                Log.w(TAG, failureCause);
                break;
            case COLLECT:
                mCollector.addMessage(failureCause);
                break;
            case ASSERT:
                Assert.fail(failureCause);
            default:
                throw new UnsupportedOperationException("Unhandled level " + mLevel);
        }
    }
}
