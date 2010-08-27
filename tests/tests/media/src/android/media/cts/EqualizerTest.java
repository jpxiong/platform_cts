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

package android.media.cts;

import android.media.AudioEffect;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.Equalizer;
import android.os.Looper;
import android.test.AndroidTestCase;
import android.util.Log;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

@TestTargetClass(Equalizer.class)
public class EqualizerTest extends AndroidTestCase {

    private String TAG = "EqualizerTest";
    private final static int MIN_NUMBER_OF_BANDS = 4;
    private final static int MAX_LEVEL_RANGE_LOW = -1200;         // -12dB
    private final static int MIN_LEVEL_RANGE_HIGH = 1200;         // +12dB
    private final static int TEST_FREQUENCY_MILLIHERTZ = 1000000; // 1kHz
    private final static int MIN_NUMBER_OF_PRESETS = 0;
    private final static float TOLERANCE = 100;                   // +/-1dB

    private Equalizer mEqualizer = null;
    private Equalizer mEqualizer2 = null;
    private int mSession = -1;
    private boolean mHasControl = false;
    private boolean mIsEnabled = false;
    private int mChangedParameter = -1;
    private boolean mInitialized = false;
    private Looper mLooper = null;
    private final Object mLock = new Object();

    private void log(String testName, String message) {
        Log.v(TAG, "[" + testName + "] " + message);
    }

    private void loge(String testName, String message) {
        Log.e(TAG, "[" + testName + "] " + message);
    }

    //-----------------------------------------------------------------
    // EQUALIZER TESTS:
    //----------------------------------

    //-----------------------------------------------------------------
    // 0 - constructor
    //----------------------------------

    //Test case 0.0: test constructor and release
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "Equalizer",
            args = {int.class, int.class}
        ),
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "getId",
                args = {}
        ),
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "release",
                args = {}
        )
    })
    public void test0_0ConstructorAndRelease() throws Exception {
        boolean result = false;
        String msg = "test1_0ConstructorAndRelease()";
        Equalizer eq = null;
         try {
            eq = new Equalizer(0, 0);
            assertNotNull(msg + ": could not create Equalizer", eq);
            try {
                assertTrue(msg +": invalid effect ID", (eq.getId() != 0));
            } catch (IllegalStateException e) {
                msg = msg.concat(": Equalizer not initialized");
            }
            result = true;
        } catch (IllegalArgumentException e) {
            msg = msg.concat(": Equalizer not found");
        } catch (UnsupportedOperationException e) {
            msg = msg.concat(": Effect library not loaded");
        } finally {
            if (eq != null) {
                eq.release();
            }
        }
        assertTrue(msg, result);
    }


    //-----------------------------------------------------------------
    // 1 - get/set parameters
    //----------------------------------

    //Test case 1.0: test setBandLevel() and getBandLevel()
    @TestTargets({
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "getNumberOfBands",
                args = {}
        ),
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "getBandLevelRange",
                args = {}
        ),
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "setBandLevel",
                args = {short.class, short.class}
        ),
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "getBandLevel",
                args = {short.class}
        )
    })
    public void test1_0BandLevel() throws Exception {
        boolean result = false;
        String msg = "test1_0BandLevel()";
        getEqualizer(0);
        try {
            short numBands = mEqualizer.getNumberOfBands();
            assertTrue(msg + ": not enough bands", numBands >= MIN_NUMBER_OF_BANDS);

            short[] levelRange = mEqualizer.getBandLevelRange();
            assertTrue(msg + ": min level too high", levelRange[0] <= MAX_LEVEL_RANGE_LOW);
            assertTrue(msg + ": max level too low", levelRange[1] >= MIN_LEVEL_RANGE_HIGH);

            mEqualizer.setBandLevel((short)0, levelRange[1]);
            short level = mEqualizer.getBandLevel((short)0);
            // allow +/- TOLERANCE margin on actual level compared to requested level
            assertTrue(msg + ": setBandLevel failed",
                    (level >= (levelRange[1] - TOLERANCE)) &&
                    (level <= (levelRange[1] + TOLERANCE)));
            result = true;
        } catch (IllegalArgumentException e) {
            msg = msg.concat(": Bad parameter value");
            loge(msg, "Bad parameter value");
        } catch (UnsupportedOperationException e) {
            msg = msg.concat(": get parameter() rejected");
            loge(msg, "get parameter() rejected");
        } catch (IllegalStateException e) {
            msg = msg.concat("get parameter() called in wrong state");
            loge(msg, "get parameter() called in wrong state");
        } finally {
            releaseEqualizer();
        }
        assertTrue(msg, result);
    }

    //Test case 1.1: test band frequency
    @TestTargets({
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "getBand",
                args = {int.class}
        ),
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "getBandFreqRange",
                args = {short.class}
        ),
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "getCenterFreq",
                args = {short.class}
        )
    })
    public void test1_1BandFrequency() throws Exception {
        boolean result = false;
        String msg = "test1_1BandFrequency()";
        getEqualizer(0);
        try {
            short band = mEqualizer.getBand(TEST_FREQUENCY_MILLIHERTZ);
            assertTrue(msg + ": getBand failed", band >= 0);
            int[] freqRange = mEqualizer.getBandFreqRange(band);
            assertTrue(msg + ": getBandFreqRange failed",
                    (freqRange[0] <= TEST_FREQUENCY_MILLIHERTZ) &&
                    (freqRange[1] >= TEST_FREQUENCY_MILLIHERTZ));
            int freq = mEqualizer.getCenterFreq(band);
            assertTrue(msg + ": getCenterFreq failed",
                    (freqRange[0] <= freq) && (freqRange[1] >= freq));
            result = true;
        } catch (IllegalArgumentException e) {
            msg = msg.concat(": Bad parameter value");
            loge(msg, "Bad parameter value");
        } catch (UnsupportedOperationException e) {
            msg = msg.concat(": get parameter() rejected");
            loge(msg, "get parameter() rejected");
        } catch (IllegalStateException e) {
            msg = msg.concat("get parameter() called in wrong state");
            loge(msg, "get parameter() called in wrong state");
        } finally {
            releaseEqualizer();
        }
        assertTrue(msg, result);
    }

    //Test case 1.2: test presets
    @TestTargets({
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "getNumberOfPresets",
                args = {}
        ),
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "usePreset",
                args = {short.class}
        ),
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "getCurrentPreset",
                args = {}
        ),
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "getPresetName",
                args = {short.class}
        )
    })
    public void test1_2Presets() throws Exception {
        boolean result = false;
        String msg = "test1_2Presets()";
        getEqualizer(0);
        try {
            short numPresets = mEqualizer.getNumberOfPresets();
            assertTrue(msg + ": getNumberOfPresets failed", numPresets >= MIN_NUMBER_OF_PRESETS);
            if (numPresets > 0) {
                mEqualizer.usePreset((short)(numPresets - 1));
                short preset = mEqualizer.getCurrentPreset();
                assertEquals(msg + ": usePreset failed", preset, (short)(numPresets - 1));
                String name = mEqualizer.getPresetName(preset);
                assertNotNull(msg + ": getPresetName failed", name);
            }
            result = true;
        } catch (IllegalArgumentException e) {
            msg = msg.concat(": Bad parameter value");
            loge(msg, "Bad parameter value");
        } catch (UnsupportedOperationException e) {
            msg = msg.concat(": get parameter() rejected");
            loge(msg, "get parameter() rejected");
        } catch (IllegalStateException e) {
            msg = msg.concat("get parameter() called in wrong state");
            loge(msg, "get parameter() called in wrong state");
        } finally {
            releaseEqualizer();
        }
        assertTrue(msg, result);
    }

    //Test case 1.3: test properties
    @TestTargets({
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "getProperties",
                args = {}
        ),
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "setProperties",
                args = {Equalizer.Settings.class}
        )
    })
    public void test1_3Properties() throws Exception {
        boolean result = false;
        String msg = "test1_3Properties()";
        getEqualizer(0);
        try {
            Equalizer.Settings settings = mEqualizer.getProperties();
            assertTrue(msg + ": no enough bands", settings.numBands >= MIN_NUMBER_OF_BANDS);
            short newLevel = 0;
            if (settings.bandLevels[0] == 0) {
                newLevel = -600;
            }
            String str = settings.toString();
            settings = new Equalizer.Settings(str);
            settings.curPreset = (short)-1;
            settings.bandLevels[0] = newLevel;
            mEqualizer.setProperties(settings);
            settings = mEqualizer.getProperties();
            Log.e(TAG, "settings.bandLevels[0]: "+settings.bandLevels[0]+" newLevel: "+newLevel);
            assertTrue(msg + ": setProperties failed",
                    (settings.bandLevels[0] >= (newLevel - TOLERANCE)) &&
                    (settings.bandLevels[0] <= (newLevel + TOLERANCE)));

            result = true;
        } catch (IllegalArgumentException e) {
            msg = msg.concat(": Bad parameter value");
            loge(msg, "Bad parameter value");
        } catch (UnsupportedOperationException e) {
            msg = msg.concat(": get parameter() rejected");
            loge(msg, "get parameter() rejected");
        } catch (IllegalStateException e) {
            msg = msg.concat("get parameter() called in wrong state");
            loge(msg, "get parameter() called in wrong state");
        } finally {
            releaseEqualizer();
        }
        assertTrue(msg, result);
    }

    //Test case 1.4: test setBandLevel() throws exception after release
    @TestTargets({
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "release",
                args = {}
        ),
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "setBandLevel",
                args = {short.class, short.class}
        )
    })
    public void test1_4SetBandLevelAfterRelease() throws Exception {
        boolean result = false;
        String msg = "test1_4SetBandLevelAfterRelease()";

        getEqualizer(0);
        mEqualizer.release();
        try {
            mEqualizer.setBandLevel((short)0, (short)0);
        } catch (IllegalStateException e) {
            result = true;
        } finally {
            releaseEqualizer();
        }
        assertTrue(msg+ ": no exception for setBandLevel() after release()", result);
    }

    //-----------------------------------------------------------------
    // 2 - Effect enable/disable
    //----------------------------------

    //Test case 2.0: test setEnabled() and getEnabled() in valid state
    @TestTargets({
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "setEnabled",
                args = {boolean.class}
        ),
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "getEnabled",
                args = {}
        )
    })
    public void test2_0SetEnabledGetEnabled() throws Exception {
        boolean result = false;
        String msg = "test2_0SetEnabledGetEnabled()";

        getEqualizer(0);
        try {
            mEqualizer.setEnabled(true);
            assertTrue(msg + ": invalid state from getEnabled", mEqualizer.getEnabled());
            mEqualizer.setEnabled(false);
            assertFalse(msg + ": invalid state to getEnabled", mEqualizer.getEnabled());
            result = true;
        } catch (IllegalStateException e) {
            msg = msg.concat(": setEnabled() in wrong state");
        } finally {
            releaseEqualizer();
        }
        assertTrue(msg, result);
    }

    //Test case 2.1: test setEnabled() throws exception after release
    @TestTargets({
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "release",
                args = {}
        ),
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                method = "setEnabled",
                args = {boolean.class}
        )
    })
    public void test2_1SetEnabledAfterRelease() throws Exception {
        boolean result = false;
        String msg = "test2_1SetEnabledAfterRelease()";

        getEqualizer(0);
        mEqualizer.release();
        try {
            mEqualizer.setEnabled(true);
        } catch (IllegalStateException e) {
            result = true;
        } finally {
            releaseEqualizer();
        }
        assertTrue(msg+ ": no exception for setEnabled() after release()", result);
    }

    //-----------------------------------------------------------------
    // 3 priority and listeners
    //----------------------------------

    //Test case 3.0: test control status listener
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "setControlStatusListener",
            args = {AudioEffect.OnControlStatusChangeListener.class}
        )
    })
    public void test3_0ControlStatusListener() throws Exception {
        boolean result = false;
        String msg = "test3_0ControlStatusListener()";
        mHasControl = true;
        createListenerLooper(true, false, false);
        synchronized(mLock) {
            try {
                mLock.wait(1000);
            } catch(Exception e) {
                Log.e(TAG, "Looper creation: wait was interrupted.");
            }
        }
        assertTrue(mInitialized);
        synchronized(mLock) {
            try {
                getEqualizer(0);
                mLock.wait(1000);
            } catch(Exception e) {
                Log.e(TAG, "Create second effect: wait was interrupted.");
            } finally {
                releaseEqualizer();
                terminateListenerLooper();
            }
        }
        assertFalse(msg + ": effect control not lost by effect1", mHasControl);
        result = true;
        assertTrue(msg, result);
    }

    //Test case 3.1: test enable status listener
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "setEnableStatusListener",
            args = {AudioEffect.OnEnableStatusChangeListener.class}
        )
    })
    public void test3_1EnableStatusListener() throws Exception {
        boolean result = false;
        String msg = "test3_1EnableStatusListener()";
        createListenerLooper(false, true, false);
        synchronized(mLock) {
            try {
                mLock.wait(1000);
            } catch(Exception e) {
                Log.e(TAG, "Looper creation: wait was interrupted.");
            }
        }
        assertTrue(mInitialized);
        mEqualizer2.setEnabled(true);
        mIsEnabled = true;
        getEqualizer(0);
        synchronized(mLock) {
            try {
                mEqualizer.setEnabled(false);
                mLock.wait(1000);
            } catch(Exception e) {
                Log.e(TAG, "Create second effect: wait was interrupted.");
            } finally {
                releaseEqualizer();
                terminateListenerLooper();
            }
        }
        assertFalse(msg + ": enable status not updated", mIsEnabled);
        result = true;
        assertTrue(msg, result);
    }

    //Test case 3.2: test parameter changed listener
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "setParameterListener",
            args = {Equalizer.OnParameterChangeListener.class}
        )
    })
    public void test3_2ParameterChangedListener() throws Exception {
        boolean result = false;
        String msg = "test3_2ParameterChangedListener()";
        createListenerLooper(false, false, true);
        synchronized(mLock) {
            try {
                mLock.wait(1000);
            } catch(Exception e) {
                Log.e(TAG, "Looper creation: wait was interrupted.");
            }
        }
        assertTrue(mInitialized);
        getEqualizer(0);
        synchronized(mLock) {
            try {
                mChangedParameter = -1;
                mEqualizer.setBandLevel((short)0, (short)0);
                mLock.wait(1000);
            } catch(Exception e) {
                Log.e(TAG, "Create second effect: wait was interrupted.");
            } finally {
                releaseEqualizer();
                terminateListenerLooper();
            }
        }
        assertEquals(msg + ": parameter change not received",
                Equalizer.PARAM_BAND_LEVEL, mChangedParameter);
        result = true;
        assertTrue(msg, result);
    }

    //-----------------------------------------------------------------
    // private methods
    //----------------------------------

    private void getEqualizer(int session) {
         if (mEqualizer == null || session != mSession) {
             if (session != mSession && mEqualizer != null) {
                 mEqualizer.release();
                 mEqualizer = null;
             }
             try {
                mEqualizer = new Equalizer(0, session);
                mSession = session;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "getEqualizer() Equalizer not found exception: "+e);
            } catch (UnsupportedOperationException e) {
                Log.e(TAG, "getEqualizer() Effect library not loaded exception: "+e);
            }
         }
         assertNotNull("could not create mEqualizer", mEqualizer);
    }

    private void releaseEqualizer() {
        if (mEqualizer != null) {
            mEqualizer.release();
            mEqualizer = null;
        }
    }

    // Initializes the equalizer listener looper
    class ListenerThread extends Thread {
        boolean mControl;
        boolean mEnable;
        boolean mParameter;

        public ListenerThread(boolean control, boolean enable, boolean parameter) {
            super();
            mControl = control;
            mEnable = enable;
            mParameter = parameter;
        }
    }

    private void createListenerLooper(boolean control, boolean enable, boolean parameter) {

        new ListenerThread(control, enable, parameter) {
            @Override
            public void run() {
                // Set up a looper
                Looper.prepare();

                // Save the looper so that we can terminate this thread
                // after we are done with it.
                mLooper = Looper.myLooper();

                mEqualizer2 = new Equalizer(0, 0);
                assertNotNull("could not create Equalizer2", mEqualizer2);

                if (mControl) {
                    mEqualizer2.setControlStatusListener(
                            new AudioEffect.OnControlStatusChangeListener() {
                        public void onControlStatusChange(
                                AudioEffect effect, boolean controlGranted) {
                            synchronized(mLock) {
                                if (effect == mEqualizer2) {
                                    mHasControl = controlGranted;
                                    mLock.notify();
                                }
                            }
                        }
                    });
                }
                if (mEnable) {
                    mEqualizer2.setEnableStatusListener(
                            new AudioEffect.OnEnableStatusChangeListener() {
                        public void onEnableStatusChange(AudioEffect effect, boolean enabled) {
                            synchronized(mLock) {
                                if (effect == mEqualizer2) {
                                    mIsEnabled = enabled;
                                    mLock.notify();
                                }
                            }
                        }
                    });
                }
                if (mParameter) {
                    mEqualizer2.setParameterListener(new Equalizer.OnParameterChangeListener() {
                        public void onParameterChange(Equalizer effect,
                                int status, int param1, int param2, int value)
                        {
                            synchronized(mLock) {
                                if (effect == mEqualizer2) {
                                    mChangedParameter = param1;
                                    mLock.notify();
                                }
                            }
                        }
                    });
                }

                synchronized(mLock) {
                    mInitialized = true;
                    mLock.notify();
                }
                Looper.loop();  // Blocks forever until Looper.quit() is called.
            }
        }.start();
    }

    // Terminates the listener looper thread.
    private void terminateListenerLooper() {
        if (mEqualizer2 != null) {
            mEqualizer2.release();
            mEqualizer2 = null;
        }
        if (mLooper != null) {
            mLooper.quit();
            mLooper = null;
        }
    }

}