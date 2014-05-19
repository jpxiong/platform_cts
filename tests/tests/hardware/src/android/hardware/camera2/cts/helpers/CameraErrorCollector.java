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

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.CaptureResult;
import android.util.Log;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.rules.ErrorCollector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A camera test ErrorCollector class to gather the test failures during a test,
 * instead of failing the test immediately for each failure.
 */
public class CameraErrorCollector extends ErrorCollector {

    private static final String TAG = "CameraErrorCollector";
    private static final boolean LOG_ERRORS = Log.isLoggable(TAG, Log.ERROR);

    private String mCameraMsg = "";

    @Override
    public void verify() throws Throwable {
        // Do not remove if using JUnit 3 test runners. super.verify() is protected.
        super.verify();
    }

    /**
     * Adds an unconditional error to the table. Execution continues, but test will fail at the end.
     *
     * @param message A string containing the failure reason.
     */
    public void addMessage(String message) {
        addErrorSuper(new Throwable(mCameraMsg + message));
    }

    /**
     * Adds a Throwable to the table.  Execution continues, but the test will fail at the end.
     */
    @Override
    public void addError(Throwable error) {
        addErrorSuper(new Throwable(mCameraMsg + error.getMessage(), error));
    }

    private void addErrorSuper(Throwable error) {
        if (LOG_ERRORS) Log.e(TAG, error.getMessage());
        super.addError(error);
    }

    /**
     * Adds a failure to the table if {@code matcher} does not match {@code value}.
     * Execution continues, but the test will fail at the end if the match fails.
     * The camera id is included into the failure log.
     */
    @Override
    public <T> void checkThat(final T value, final Matcher<T> matcher) {
        super.checkThat(mCameraMsg, value, matcher);
    }

    /**
     * Adds a failure with the given {@code reason} to the table if
     * {@code matcher} does not match {@code value}. Execution continues, but
     * the test will fail at the end if the match fails. The camera id is
     * included into the failure log.
     */
    @Override
    public <T> void checkThat(final String reason, final T value, final Matcher<T> matcher) {
        super.checkThat(mCameraMsg + reason, value, matcher);

    }

    /**
     * Set the camera id to this error collector object for logging purpose.
     *
     * @param id The camera id to be set.
     */
    public void setCameraId(String id) {
        if (id != null) {
            mCameraMsg = "Test failed for camera " + id + ": ";
        } else {
            mCameraMsg = "";
        }
    }

    /**
     * Adds a failure to the table if {@code condition} is not {@code true}.
     * <p>
     * Execution continues, but the test will fail at the end if the condition
     * failed.
     * </p>
     *
     * @param msg Message to be logged when check fails.
     * @param condition Log the failure if it is not true.
     */
    public boolean expectTrue(String msg, boolean condition) {
        if (!condition) {
            addMessage(msg);
        }

        return condition;
    }

    /**
     * Check if the two values are equal.
     *
     * @param msg Message to be logged when check fails.
     * @param expected Expected value to be checked against.
     * @param actual Actual value to be checked.
     * @return {@code true} if the two values are equal, {@code false} otherwise.
     */
    public <T> boolean expectEquals(String msg, T expected, T actual) {
        if (expected == null) {
            throw new IllegalArgumentException("expected value shouldn't be null");
        }

        if (!Objects.equals(expected, actual)) {
            if (actual == null) {
                addMessage(msg + ", actual value is null");
                return false;
            }

            addMessage(String.format("%s (expected = %s, actual = %s) ", msg, expected.toString(),
                    actual.toString()));
            return false;
        }

        return true;
    }

    /**
     * Check if the two arrays of values are deeply equal.
     *
     * @param msg Message to be logged when check fails.
     * @param expected Expected array of values to be checked against.
     * @param actual Actual array of values to be checked.
     * @return {@code true} if the two arrays of values are deeply equal, {@code false} otherwise.
     */
    public <T> boolean expectEquals(String msg, T[] expected, T[] actual) {
        if (expected == null) {
            throw new IllegalArgumentException("expected value shouldn't be null");
        }

        if (!Arrays.deepEquals(expected, actual)) {
            if (actual == null) {
                addMessage(msg + ", actual value is null");
                return false;
            }

            addMessage(String.format("%s (expected = %s, actual = %s) ", msg,
                    Arrays.deepToString(expected), Arrays.deepToString(actual)));
            return false;
        }

        return true;
    }

    /**
     * Check if the two float values are equal with given error tolerance.
     *
     * @param msg Message to be logged when check fails.
     * @param expected Expected value to be checked against.
     * @param actual Actual value to be checked.
     * @param tolerance The error margin for the equality check.
     * @return {@code true} if the two values are equal, {@code false} otherwise.
     */
    public <T> boolean expectEquals(String msg, float expected, float actual, float tolerance) {
        if (expected == actual) {
            return true;
        }

        if (!(Math.abs(expected - actual) <= tolerance)) {
            addMessage(String.format("%s (expected = %s, actual = %s, tolerance = %s) ", msg,
                    expected, actual, tolerance));
            return false;
        }

        return true;
    }

    /**
     * Check if the two double values are equal with given error tolerance.
     *
     * @param msg Message to be logged when check fails.
     * @param expected Expected value to be checked against.
     * @param actual Actual value to be checked.
     * @param tolerance The error margin for the equality check
     * @return {@code true} if the two values are equal, {@code false} otherwise.
     */
    public <T> boolean expectEquals(String msg, double expected, double actual, double tolerance) {
        if (expected == actual)
        {
            return true;
        }

        if (!(Math.abs(expected - actual) <= tolerance)) {
            addMessage(String.format("%s (expected = %s, actual = %s, tolerance = %s) ", msg,
                    expected, actual, tolerance));
            return false;
        }

        return true;
    }

    /**
     * Expect the list of values are in the range.
     *
     * @param msg Message to be logged
     * @param list The list of values to be checked
     * @param min The min value of the range
     * @param max The max value of the range
     */
    public <T extends Comparable<? super T>> void expectValuesInRange(String msg, List<T> list,
            T min, T max) {
        for (T value : list) {
            expectTrue(msg + String.format(", array value " + value.toString() +
                    " is out of range [%s, %s]",
                    min.toString(), max.toString()),
                    value.compareTo(max)<= 0 && value.compareTo(min) >= 0);
        }
    }

    /**
     * Expect the array of values are in the range.
     *
     * @param msg Message to be logged
     * @param array The array of values to be checked
     * @param min The min value of the range
     * @param max The max value of the range
     */
    public <T extends Comparable<? super T>> void expectValuesInRange(String msg, T[] array,
            T min, T max) {
        expectValuesInRange(msg, Arrays.asList(array), min, max);
    }

    /**
     * Expect the value is in the range.
     *
     * @param msg Message to be logged
     * @param value The value to be checked
     * @param min The min value of the range
     * @param max The max value of the range
     */
    public <T extends Comparable<? super T>> void expectInRange(String msg, T value,
            T min, T max) {
        expectTrue(msg + String.format(", value " + value.toString() + " is out of range [%s, %s]",
                min.toString(), max.toString()),
                value.compareTo(max)<= 0 && value.compareTo(min) >= 0);
    }

    public void expectNotNull(String msg, Object obj) {
        checkThat(msg, obj, CoreMatchers.notNullValue());
    }

    /**
     * Check if the values in the array are monotonically increasing (decreasing) and not all
     * equal.
     *
     * @param array The array of values to be checked
     * @param ascendingOrder The monotonicity ordering to be checked with
     */
    public <T extends Comparable<? super T>>  void checkArrayMonotonicityAndNotAllEqual(T[] array,
            boolean ascendingOrder) {
        String orderMsg = ascendingOrder ? ("increasing order") : ("decreasing order");
        for (int i = 0; i < array.length - 1; i++) {
            int compareResult = array[i + 1].compareTo(array[i]);
            boolean condition = compareResult >= 0;
            if (!ascendingOrder) {
                condition = compareResult <= 0;
            }

            expectTrue(String.format("Adjacent values (%s and %s) %s monotonicity is broken",
                    array[i].toString(), array[i + 1].toString(), orderMsg), condition);
        }

        expectTrue("All values of this array are equal: " + array[0].toString(),
                array[0].compareTo(array[array.length - 1]) != 0);
    }

    /**
     * Check if the key value is not null and return the value.
     *
     * @param request The {@link CaptureRequest#Builder} to get the key from.
     * @param key The {@link CaptureRequest} key to be checked.
     * @return The value of the key.
     */
    public <T> T expectKeyValueNotNull(Builder request, CaptureRequest.Key<T> key) {

        T value = request.get(key);
        if (value == null) {
            addMessage("Key " + key.getName() + " shouldn't be null");
        }

        return value;
    }

    /**
     * Check if the key value is not null and return the value.
     *
     * @param result The {@link CaptureResult} to get the key from.
     * @param key The {@link CaptureResult} key to be checked.
     * @return The value of the key.
     */
    public <T> T expectKeyValueNotNull(CaptureResult result, CaptureResult.Key<T> key) {
        return expectKeyValueNotNull("", result, key);
    }

    /**
     * Check if the key value is not null and return the value.
     *
     * @param msg The message to be logged.
     * @param result The {@link CaptureResult} to get the key from.
     * @param key The {@link CaptureResult} key to be checked.
     * @return The value of the key.
     */
    public <T> T expectKeyValueNotNull(String msg, CaptureResult result, CaptureResult.Key<T> key) {

        T value = result.get(key);
        if (value == null) {
            addMessage(msg + " Key " + key.getName() + " shouldn't be null");
        }

        return value;
    }

    /**
     * Check if the key is non-null and the value is not equal to target.
     *
     * @param request The The {@link CaptureRequest#Builder} to get the key from.
     * @param key The {@link CaptureRequest} key to be checked.
     * @param expected The expected value of the CaptureRequest key.
     */
    public <T> void expectKeyValueNotEquals(
            Builder request, CaptureRequest.Key<T> key, T expected) {
        if (request == null || key == null || expected == null) {
            throw new IllegalArgumentException("request, key and target shouldn't be null");
        }

        T value;
        if ((value = expectKeyValueNotNull(request, key)) == null) {
            return;
        }

        String reason = "Key " + key.getName() + " shouldn't have value " + value.toString();
        checkThat(reason, value, CoreMatchers.not(expected));
    }

    /**
     * Check if the key is non-null and the value is not equal to target.
     *
     * @param result The The {@link CaptureResult} to get the key from.
     * @param key The {@link CaptureResult} key to be checked.
     * @param expected The expected value of the CaptureResult key.
     */
    public <T> void expectKeyValueNotEquals(
            CaptureResult result, CaptureResult.Key<T> key, T expected) {
        if (result == null || key == null || expected == null) {
            throw new IllegalArgumentException("result, key and target shouldn't be null");
        }

        T value;
        if ((value = expectKeyValueNotNull(result, key)) == null) {
            return;
        }

        String reason = "Key " + key.getName() + " shouldn't have value " + value.toString();
        checkThat(reason, value, CoreMatchers.not(expected));
    }

    /**
     * Check if the key is non-null and the value is equal to target.
     *
     * <p>Only check non-null if the target is null.</p>
     *
     * @param request The The {@link CaptureRequest#Builder} to get the key from.
     * @param key The {@link CaptureRequest} key to be checked.
     * @param expected The expected value of the CaptureRequest key.
     */
    public <T> void expectKeyValueEquals(Builder request, CaptureRequest.Key<T> key, T expected) {
        if (request == null || key == null || expected == null) {
            throw new IllegalArgumentException("request, key and target shouldn't be null");
        }

        T value;
        if ((value = expectKeyValueNotNull(request, key)) == null) {
            return;
        }

        String reason = "Key " + key.getName() + " value " + value.toString()
                + " doesn't match the expected value " + expected.toString();
        checkThat(reason, value, CoreMatchers.equalTo(expected));
    }

    /**
     * Check if the element inside of the list are unique.
     *
     * @param msg The message to be logged
     * @param list The list of values to be checked
     */
    public <T> void expectValuesUnique(String msg, List<T> list) {
        Set<T> sizeSet = new HashSet<T>(list);
        expectTrue(msg + " each size must be distinct", sizeSet.size() == list.size());
    }
}
