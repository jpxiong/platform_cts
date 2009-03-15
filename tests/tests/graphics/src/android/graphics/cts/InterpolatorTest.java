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

package android.graphics.cts;

import junit.framework.TestCase;
import android.graphics.Interpolator;
import android.graphics.Interpolator.Result;
import android.os.SystemClock;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

@TestTargetClass(Interpolator.class)
public class InterpolatorTest extends TestCase {

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test constructor(s) of Interpolator.",
            method = "Interpolator",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test constructor(s) of Interpolator.",
            method = "Interpolator",
            args = {int.class, int.class}
        )
    })
    public void testConstructor() {

        // new the Interpolator instance
        new Interpolator(10);
        // new the Interpolator instance
        new Interpolator(10, 20);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test reset(int valueCount).",
            method = "reset",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test reset(int valueCount).",
            method = "getValueCount",
            args = {}
        )
    })
    public void testReset1() {
        int expected = 100;
        // new the Interpolator instance
        Interpolator interpolator = new Interpolator(10);
        interpolator.reset(expected);
        assertEquals(expected, interpolator.getValueCount());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test reset(int valueCount, int frameCount).",
            method = "reset",
            args = {int.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test reset(int valueCount, int frameCount).",
            method = "getKeyFrameCount",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test reset(int valueCount, int frameCount).",
            method = "getValueCount",
            args = {}
        )
    })
    public void testReset2() {
        int expected1 = 100;
        int expected2 = 200;
        // new the Interpolator instance
        Interpolator interpolator = new Interpolator(10);
        interpolator.reset(expected1, expected2);
        assertEquals(expected1, interpolator.getValueCount());
        assertEquals(expected2, interpolator.getKeyFrameCount());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test timeToValues(float[] values).",
            method = "timeToValues",
            args = {float[].class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test timeToValues(float[] values).",
            method = "reset",
            args = {int.class}
        )
    })
    public void testTimeToValues1() {

        // new the Interpolator instance
        Interpolator interpolator = new Interpolator(1);
        float[]f = new float[2];
        f[0] = 1.0f;
        f[1] = 2.0f;
        // result changes along with system clock
        assertNotNull(interpolator.timeToValues(f));

        f[0] = 2.0f;
        f[1] = 1.0f;
        assertNotNull(interpolator.timeToValues(f));

        f[0] = Float.MAX_VALUE;
        f[1] = Float.MIN_VALUE;
        assertNotNull(interpolator.timeToValues(f));

        assertNotNull(interpolator.timeToValues(null));

        // this function cannot assert the expected value because it use
        // System.uptimeMillis() to calculate.
        interpolator.reset(10);
        try {
            interpolator.timeToValues(f);
            fail("should throw out ArrayStoreException");
        } catch (Exception e) {
        }
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test timeToValues(int msec, float[] values).",
            method = "timeToValues",
            args = {int.class, float[].class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test timeToValues(int msec, float[] values).",
            method = "reset",
            args = {int.class}
        )
    })
    @ToBeFixed( bug = "", explanation = "This test is broken and needs to be updated.")
    public void TestTimeToValues2() {

        // new the Interpolator instance
        Interpolator interpolator = new Interpolator(1);
        float[]f = new float[2];
        f[0] = 1.0f;
        f[1] = 2.0f;
        long time = 25139237;
        assertEquals(Result.FREEZE_START, interpolator.timeToValues((int) time, f));

        f[0] = 2.0f;
        f[1] = 1.0f;
        assertEquals(Result.FREEZE_START, interpolator.timeToValues((int) time, f));

        f[0] = Float.MAX_VALUE;
        f[1] = Float.MIN_VALUE;
        assertEquals(Result.FREEZE_START, interpolator.timeToValues((int) time, f));

        assertEquals(Result.FREEZE_START, interpolator.timeToValues((int) time, null));

        f[0] = 1.0f;
        f[1] = 2.0f;
        time = 4341610519891249779l;

        f[0] = 1.0f;
        f[1] = 2.0f;
        assertEquals(Result.FREEZE_END, interpolator.timeToValues((int) time, f));

        f[0] = 2.0f;
        f[1] = 1.0f;
        assertEquals(Result.FREEZE_END, interpolator.timeToValues((int) time, f));

        f[0] = Float.MAX_VALUE;
        f[1] = Float.MIN_VALUE;
        assertEquals(Result.FREEZE_END, interpolator.timeToValues((int) time, f));

        assertEquals(Result.FREEZE_END, interpolator.timeToValues((int) time, null));

        interpolator.reset(10);
        try {
            interpolator.timeToValues((int)SystemClock.uptimeMillis(), f);
            fail("should throw out ArrayStoreException");
        } catch (Exception e) {
        }
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test finalize().",
        method = "finalize",
        args = {}
    )
    public void testFinalize() {
        // new the Interpolator instance
        MockInterpolator interpolator = new MockInterpolator(10);
        try {
            interpolator.finalize();
        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    class MockInterpolator extends Interpolator {

        public MockInterpolator(int valueCount) {
            super(valueCount);
        }

        @Override
        public void finalize() throws Throwable {
            super.finalize();
        }
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test setKeyFrame(int index, int msec, float[] values).",
        method = "setKeyFrame",
        args = {int.class, int.class, float[].class}
    )
    public void testSetKeyFrame1() {
        // new the Interpolator instance
        Interpolator interpolator = new Interpolator(2);
        float[] f = new float[3];
        interpolator.setKeyFrame(1, (int)SystemClock.uptimeMillis(), f);
        // since it calls a native function, what we do is just call it
        // and make sure it doesn't throw out exception.
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test setKeyFrame(int index, int msec, float[] values, float[] blend).",
        method = "setKeyFrame",
        args = {int.class, int.class, float[].class, float[].class}
    )
    public void testSetKeyFrame2() {
        // new the Interpolator instance
        Interpolator interpolator = new Interpolator(2);
        float[] f = new float[3];
        interpolator.setKeyFrame(1, (int)SystemClock.uptimeMillis(), f, null);
        float[] blend = new float[5];
        interpolator.setKeyFrame(1, (int)SystemClock.uptimeMillis(), f, blend);
        // since it calls a native function, what we do is just call it
        // and make sure it doesn't throw out exception.
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test setRepeatMirror(float repeatCount, boolean mirror).",
        method = "setRepeatMirror",
        args = {float.class, boolean.class}
    )
    public void testSetRepeatMirror() {
        // new the Interpolator instance
        Interpolator interpolator = new Interpolator(2);
        interpolator.setRepeatMirror(1, true);
        // since it calls a native function, what we do is just call it
        // and make sure it doesn't throw out exception.
    }

}
