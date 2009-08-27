/*
 * Copyright (C) 2009 The Android Open Source Project
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
package android.gesture.cts;

import dalvik.annotation.TestTargetClass;

import android.gesture.Gesture;
import android.gesture.GestureStore;
import android.gesture.GestureStroke;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * Compatibility unit tests for {@link GestureStore}
 */
@TestTargetClass(GestureStore.class)
public class GestureStoreTest extends TestCase {

    private GestureStore mGestureStore;
    /** Simple straight line gesture used for basic testing */
    private Gesture mLineGesture;
    private Gesture mAnotherGesture;
    private static final String TEST_GESTURE_NAME ="cts-test-gesture";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mGestureStore = new GestureStore();
        GestureStroke stroke = new LineGestureStrokeHelper().createLineGesture();
        mLineGesture = new Gesture();
        mLineGesture.addStroke(stroke);
        mAnotherGesture = new Gesture();
        mAnotherGesture.addStroke(stroke);
    }

    /**
     * Test method for {@link android.gesture.GestureStore#getGestureEntries()}.
     *
     * Simple check to verify an added gesture appears in set of gesture entries.
     */
    public void testGetGestureEntries() {
        // assert initially empty
        assertEquals(0, mGestureStore.getGestureEntries().size());
        mGestureStore.addGesture(TEST_GESTURE_NAME, mLineGesture);
        assertEquals(1, mGestureStore.getGestureEntries().size());
        assertTrue(mGestureStore.getGestureEntries().contains(TEST_GESTURE_NAME));
    }

    // TODO: add tests for recognize

    /**
     * Test method for {@link android.gesture.GestureStore#removeGesture(java.lang.String, android.gesture.Gesture)}.
     */
    public void testRemoveGesture() {
        mGestureStore.addGesture(TEST_GESTURE_NAME, mLineGesture);
        mGestureStore.addGesture(TEST_GESTURE_NAME, mAnotherGesture);
        mGestureStore.removeGesture(TEST_GESTURE_NAME, mAnotherGesture);
        // check that gesture just removed is gone
        assertFalse(mGestureStore.getGestures(TEST_GESTURE_NAME).contains(mAnotherGesture));

        mGestureStore.removeGesture(TEST_GESTURE_NAME, mLineGesture);
        // test that entry itself is removed
        assertFalse(mGestureStore.getGestureEntries().contains(TEST_GESTURE_NAME));
    }

    /**
     * Test method for {@link android.gesture.GestureStore#removeEntry(java.lang.String)}.
     */
    public void testRemoveEntry() {
        mGestureStore.addGesture(TEST_GESTURE_NAME, mLineGesture);
        mGestureStore.addGesture(TEST_GESTURE_NAME, mAnotherGesture);
        mGestureStore.removeEntry(TEST_GESTURE_NAME);
        assertFalse(mGestureStore.getGestureEntries().contains(TEST_GESTURE_NAME));
        assertNull(mGestureStore.getGestures(TEST_GESTURE_NAME));
    }

    /**
     * Test method for {@link android.gesture.GestureStore#getGestures(java.lang.String)}.
     */
    public void testGetGestures() {
        // test getting gestures for non existent entry
        assertNull(mGestureStore.getGestures(TEST_GESTURE_NAME));
        mGestureStore.addGesture(TEST_GESTURE_NAME, mLineGesture);
        assertTrue(mGestureStore.getGestures(TEST_GESTURE_NAME).contains(mLineGesture));
    }

    /**
     * Verify that adding a gesture is flagged as change.
     * Tests {@link android.gesture.GestureStore#hasChanged()}.
     */
    public void testHasChanged_add() {
        assertFalse(mGestureStore.hasChanged());
        mGestureStore.addGesture(TEST_GESTURE_NAME, mLineGesture);
        assertTrue(mGestureStore.hasChanged());
    }

    /**
     * Verify that removing a gesture is flagged as a change.
     * Tests {@link android.gesture.GestureStore#hasChanged()}.
     */
    public void testHasChanged_removeGesture() throws IOException {
        mGestureStore.addGesture(TEST_GESTURE_NAME, mLineGesture);
        // save gesture to clear flag
        mGestureStore.save(new ByteArrayOutputStream());
        assertFalse(mGestureStore.hasChanged());
        mGestureStore.removeGesture(TEST_GESTURE_NAME, mLineGesture);
        assertTrue(mGestureStore.hasChanged());
    }

    /**
     * Verify that removing an entry is flagged as a change.
     * Tests {@link android.gesture.GestureStore#hasChanged()}.
     */
    public void testHasChanged_removeEntry() throws IOException {
        mGestureStore.addGesture(TEST_GESTURE_NAME, mLineGesture);
        // save gesture to clear flag
        mGestureStore.save(new ByteArrayOutputStream());
        assertFalse(mGestureStore.hasChanged());
        mGestureStore.removeEntry(TEST_GESTURE_NAME);
        assertTrue(mGestureStore.hasChanged());
    }


    /**
     * Test method for {@link android.gesture.GestureStore#save(java.io.OutputStream)} and
     * {@link android.gesture.GestureStore#load(java.io.InputStream)}.
     * <p/>
     * Verifies that a simple GestureStore can be stored and retrieved from a stream
     */
    public void testSaveLoadOutputStream() throws IOException {
        ByteArrayOutputStream outStream = null;
        ByteArrayInputStream inStream = null;

        try {
            mGestureStore.addGesture(TEST_GESTURE_NAME, mLineGesture);
            outStream = new ByteArrayOutputStream();
            mGestureStore.save(outStream);

            // now load a store from the stream and verify its the same
            inStream = new ByteArrayInputStream(outStream.toByteArray());
            GestureStore loadStore = new GestureStore();
            loadStore.load(inStream);
            assertEquals(mGestureStore.getOrientationStyle(), loadStore.getOrientationStyle());
            assertEquals(mGestureStore.getSequenceType(), loadStore.getSequenceType());
            assertEquals(mGestureStore.getGestureEntries(), loadStore.getGestureEntries());
            Gesture loadedGesture = loadStore.getGestures(TEST_GESTURE_NAME).get(0);
            new GestureComparator().assertGesturesEquals(mLineGesture, loadedGesture);

        } finally {
            if (inStream != null) {
                inStream.close();
            }
            if (outStream != null) {
                outStream.close();
            }
        }
    }
}
