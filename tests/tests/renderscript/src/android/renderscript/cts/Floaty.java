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
 * This class keeps track of a floating point value and the computation error that has accumulated
 * in creating this number.  We also provide the four basic arithmetic operators and sqrt that
 * compute the correct error of the result.
 */
public class Floaty {
    private double mValue;  // The value this instance represent.
    private double mError;  // The real value should be between mValue - mError and mValue + mError.
    /* The number of bits the value should have, either 32 or 64.  It would have been nice to
     * use generics, e.g. Floaty<float> and Floaty<double> but Java does not support generics
     * of float and double.  Also, Java does not have a f16 type.  This can simulate it, although
     * more work will be needed.
     */
    private int mNumberOfBits;

    static private boolean relaxed;  // Whether we are doing relaxed precision computations.

    static public void setRelaxed(boolean value) {
        relaxed = value;
    }

    public Floaty(Floaty a) {
        mValue = a.mValue;
        mError = a.mError;
        mNumberOfBits = a.mNumberOfBits;
    }

    public Floaty(float a) {
        mValue = a;
        mNumberOfBits = 32;
        setErrorFromValue(1, 1);
    }

    public Floaty(double a) {
        mValue = a;
        mNumberOfBits = 64;
        setErrorFromValue(1, 1);
    }

    /** Sets the value and the error based on whether we're doing relaxed computations or not. */
    public Floaty(float v, int ulpFactor, int ulpRelaxedFactor) {
        mValue = v;
        mNumberOfBits = 32;
        setErrorFromValue(ulpFactor, ulpRelaxedFactor);
    }

    public Floaty(double v, int ulpFactor, int ulpRelaxedFactor) {
        mValue = v;
        mNumberOfBits = 64;
        setErrorFromValue(ulpFactor, ulpRelaxedFactor);
    }

    public float getFloatValue() { return (float) mValue; }
    public double getDoubleValue() { return mValue; }

    public float getFloatError() { return (float) mError; }
    public double getDoubleError() { return mError; }

    /** Returns the number we would need to multiply the ulp to get the current error. */
    public int getUlf() {
        return (int) Math.abs(mError / getUlp());
    }

    /** Returns the unit of least precision for the number we handle. */
    private double getUlp() {
        if (mNumberOfBits == 64) {
            return Math.ulp(mValue);
        } else {
            return Math.ulp((float) mValue);
        }
    }

    /**
     * Set mError to be the appropriate factor multiplied by the Unit of Least Precision (ulp)
     * of the current value.
     */
    private void setErrorFromValue(int ulpFactor, int ulpRelaxedFactor) {
        int factor = relaxed ? ulpRelaxedFactor : ulpFactor;
        mError = getUlp() * factor;
    }

    /**
     * Creates a new Floaty as the result of a computation.  The precision factor are those
     * associated with the operation just completed.
     */
    private Floaty(double actual, double valueMinusError, double valuePlusError, int ulpFactor,
            int ulpRelaxedFactor, int numberOfBits) {
        mValue = actual;
        mError = 0f;
        expandError(valueMinusError);
        expandError(valuePlusError);
        mError *= relaxed ? ulpRelaxedFactor : ulpFactor;
        mNumberOfBits = numberOfBits;
    }
    
    /** If needed, increases the error so that the provided value is covered by the error range. */
    private void expandError(double valueWithError) {
        // We disregard NaN values that can be produced when testing close to a cliff.
        if (valueWithError != valueWithError) {
            return;
        }
        double delta = Math.abs(valueWithError - mValue);
        if (delta > mError) {
            mError = delta;
        }
    }

    /** Returns true if the number passed is within mError of our value. */
    public boolean couldBe(double a) {
        return couldBe(a, 0.0);
    }

    /**
     * Returns true if the number passed is within mError of our value, or if it's whithin
     * minimumError of the value.
     */ 
    public boolean couldBe(double a, double minimumError) {
        if (a != a && mValue != mValue) {
            return true;  // Both are NaN
        }
        /* Handle the simple case.  This may not be covered by the next test if mError is NaN.
         */
        if (a == mValue) {
            return true;
        }
        double error = Math.max(mError, minimumError);
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
        return String.format("%24.9g (%16x) +- %24.9g ulf %d (%d bits)", mValue,
                Double.doubleToRawLongBits(mValue), mError, getUlf(), mNumberOfBits);
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
        double num = Math.abs(mValue);
        double den = Math.abs(a.mValue);
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
            Math.sqrt(a.mValue),
            Math.sqrt(a.mValue - a.mError),
            Math.sqrt(a.mValue + a.mError),
            3, 10, a.mNumberOfBits);
        return f;
    }
}
