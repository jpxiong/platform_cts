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

package android.renderscript.cts;

import android.util.Log;

/**
 * This class keeps track of a float and the computation error that has accumulated in creating
 * this number.  We also provide the four basic arithmetic operators and sqrt that compute the
 * correct error of the result.
 */
public class Floaty {
    private float mValue;  // The value this float reprensent.
    private float mError;  // The real value should be between mValue - mError and mValue + mError.

    static private boolean relaxed;  // Whether we are doing relaxed precision computations.

    static public void setRelaxed(boolean value) {
        relaxed = value;
    }

    public Floaty(Floaty a) {
        mValue = a.mValue;
        mError = a.mError;
    }

    public Floaty(float a) {
        mValue = a;
        setErrorFromValue(1, 1);
    }

    /** Sets the value and the error based on whether we're doing relaxed computations or not. */
    public Floaty(float v, int ulpFactor, int ulpRelaxedFactor) {
        mValue = v;
        setErrorFromValue(ulpFactor, ulpRelaxedFactor);
    }

    public float getValue() { return mValue; }

    public float getError() { return mError; }

    /** Returns the number we would need to multiply the ulp to get the current error. */
    public int getUlf() {
        return (int) Math.abs(mError / Math.ulp(mValue));
    }

    /**
     * Set mError to be the appropriate factor multiplied by the Unit of Least Precision (ulp)
     * of the current value.
     */
    private void setErrorFromValue(int ulpFactor, int ulpRelaxedFactor) {
        int factor = relaxed ? ulpRelaxedFactor : ulpFactor;
        mError = Math.ulp(mValue) * factor;
    }

    /**
     * Creates a new Floaty as the result of a computation.  The precision factor are those
     * associated with the operation just completed.
     */
    public Floaty(float actual, float valueMinusError, float valuePlusError, int ulpFactor,
            int ulpRelaxedFactor) {
        mValue = actual;
        mError = 0f;
        expandError(valueMinusError);
        expandError(valuePlusError);
        mError *= relaxed ? ulpRelaxedFactor : ulpFactor;
    }
    
    /** If needed, increases the error so that the provided value is covered by the error range. */
    private void expandError(float valueWithError) {
        // We disregard NaN values that can be produced when testing close to a cliff.
        if (valueWithError != valueWithError) {
            return;
        }
        float delta = Math.abs(valueWithError - mValue);
        if (delta > mError) {
            mError = delta;
        }
    }

    /** Returns true if the number passed is within mError of our value. */
    public boolean couldBe(float a) {
        return couldBe(a, 0.0);
    }

    /**
     * Returns true if the number passed is within mError of our value, or if it's whithin
     * minimumError of the value.
     */ 
    public boolean couldBe(float a, double minimumError) {
        if (a != a && mValue != mValue) {
            return true;  // Both are NaN
        }
        /* Handle the simple case.  This may not be covered by the next test if mError is NaN.
         */
        if (a == mValue) {
            return true;
        }
        float error = (float) Math.max(mError, minimumError);
        boolean inRange = mValue - error <= a && a <= mValue + error;

        /* This is useful for debugging:
        if (!inRange) {
            int ulfNeeded = (int) Math.abs(Math.round((a - mValue) / Math.ulp(mValue)));
            Log.e("Floaty.couldBe", "Comparing " + Float.toString(a) +
                    " against " + Float.toString(mValue) + " +- " + Float.toString(error) +
                    " relaxed " + Boolean.toString(relaxed) +
                    " ulfNeeded " + Integer.toString(ulfNeeded) +
                    ", off by " + Integer.toString(ulfNeeded - getUlf()));
        }
        */
        return inRange;
    }

    public String toString() {
        return String.format("%14.9g (%8x) +- %14.9g ulf %d", mValue,
                Float.floatToRawIntBits(mValue), mError, getUlf());
    }

    public boolean isNaN() {
        return mValue == Float.NaN;
    }

    public Floaty add(Floaty a) {
        mValue += a.mValue;
        mError += a.mError;
        return this;
    }

    public Floaty subtract(Floaty a) {
        mValue -= a.mValue;
        mError += a.mError;
        return this;
    }

    public Floaty multiply(Floaty a) {
        /* Theoretically, it should be also + a.mError * mError but that should be too small
         * to matter.
         */
        mError = Math.abs(mValue) * a.mError + Math.abs(a.mValue) * mError;
        mValue *= a.mValue;
        return this;
    }

    public Floaty divide(Floaty a) {
        float num = Math.abs(mValue);
        float den = Math.abs(a.mValue);
        mError = (num * a.mError + den * mError) / (den * (den - a.mError));
        mValue /= a.mValue;
        return this;
    }

    static public Floaty add(Floaty a, Floaty b) {
        return new Floaty(a).add(b);
    }

    static public Floaty subtract(Floaty a, Floaty b) {
        return new Floaty(a).subtract(b);
    }

    static public Floaty multiply(Floaty a, Floaty b) {
        return new Floaty(a).multiply(b);
    }

    static public Floaty divide(Floaty a, Floaty b) {
        return new Floaty(a).divide(b);
    }

    static public Floaty abs(Floaty a) {
        Floaty c = new Floaty(Math.abs(a.mValue));
        c.mError = a.mError;
        return c;
    }

    static public Floaty sqrt(Floaty a) {
        Floaty f = new Floaty(
            (float) Math.sqrt(a.mValue),
            (float) Math.sqrt(a.mValue - a.mError),
            (float) Math.sqrt(a.mValue + a.mError),
            3, 10);
        return f;
    }
}
