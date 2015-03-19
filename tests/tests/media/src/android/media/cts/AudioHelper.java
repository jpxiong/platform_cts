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

package android.media.cts;

import android.os.Looper;

// Used for statistics and loopers in listener tests.
// See AudioRecordTest.java and AudioTrack_ListenerTest.java.
public class AudioHelper {
    public static class Statistics {
        public void add(double value) {
            final double absValue = Math.abs(value);
            mSum += value;
            mSumAbs += absValue;
            mMaxAbs = Math.max(mMaxAbs, absValue);
            ++mCount;
        }

        public double getAvg() {
            if (mCount == 0) {
                return 0;
            }
            return mSum / mCount;
        }

        public double getAvgAbs() {
            if (mCount == 0) {
                return 0;
            }
            return mSumAbs / mCount;
        }

        public double getMaxAbs() {
            return mMaxAbs;
        }

        private int mCount = 0;
        private double mSum = 0;
        private double mSumAbs = 0;
        private double mMaxAbs = 0;
    }

    // for listener tests
    // lightweight java.util.concurrent.Future*
    public static class FutureLatch<T>
    {
        private T mValue;
        private boolean mSet;
        public void set(T value)
        {
            synchronized (this) {
                assert !mSet;
                mValue = value;
                mSet = true;
                notify();
            }
        }
        public T get()
        {
            T value;
            synchronized (this) {
                while (!mSet) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        ;
                    }
                }
                value = mValue;
            }
            return value;
        }
    }

    // for listener tests
    // represents a factory for T
    public interface MakesSomething<T>
    {
        T makeSomething();
    }

    // for listener tests
    // used to construct an object in the context of an asynchronous thread with looper
    public static class MakeSomethingAsynchronouslyAndLoop<T>
    {
        private Thread mThread;
        volatile private Looper mLooper;
        private final MakesSomething<T> mWhatToMake;

        public MakeSomethingAsynchronouslyAndLoop(MakesSomething<T> whatToMake)
        {
            assert whatToMake != null;
            mWhatToMake = whatToMake;
        }

        public T make()
        {
            final FutureLatch<T> futureLatch = new FutureLatch<T>();
            mThread = new Thread()
            {
                @Override
                public void run()
                {
                    Looper.prepare();
                    mLooper = Looper.myLooper();
                    T something = mWhatToMake.makeSomething();
                    futureLatch.set(something);
                    Looper.loop();
                }
            };
            mThread.start();
            return futureLatch.get();
        }
        public void join()
        {
            mLooper.quit();
            try {
                mThread.join();
            } catch (InterruptedException e) {
                ;
            }
            // avoid dangling references
            mLooper = null;
            mThread = null;
        }
    }
}
