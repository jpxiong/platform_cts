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

package android.hardware.cts.helpers.sensoroperations;

import android.hardware.cts.helpers.SensorStats;

/**
 * Base class used by all sensor operations. This allows for complex operations such as chaining
 * operations together or running operations in parallel.
 * <p>
 * Certain restrictions exist for {@link SensorOperation}s:
 * <p><ul>
 * <li>{@link #execute()} should only be called once and behavior is undefined for subsequent calls.
 * Once {@link #execute()} is called, the class should not be modified. Generally, there is no
 * synchronization for operations.</li>
 * <li>{@link #getStats()} should only be called after {@link #execute()}. If it is called before,
 * the returned value is undefined.</li>
 * <li>{@link #clone()} may be called any time and should return an operation with the same
 * parameters as the original.</li>
 * </ul>
 */
public abstract class SensorOperation {
    private final SensorStats mStats;

    protected SensorOperation() {
        this(new SensorStats());
    }

    protected SensorOperation(SensorStats stats) {
        mStats = stats;
    }

    /**
     * @return The {@link SensorStats} for the operation.
     */
    public SensorStats getStats() {
        return mStats;
    }

    /**
     * Executes the sensor operation.
     * This may throw {@link RuntimeException}s such as {@link AssertionError}s.
     *
     * NOTE: the operation is expected to handle interruption by:
     * - cleaning up on {@link InterruptedException}
     * - propagating the exception down the stack
     */
    public abstract void execute() throws InterruptedException;

    /**
     * @return The cloned {@link SensorOperation}.
     *
     * NOTE: The implementation should also clone all child operations, so that a cloned operation
     * will run with the exact same parameters as the original. The stats should not be cloned.
     */
    public abstract SensorOperation clone();

    /**
     * Wrapper around {@link SensorStats#addSensorStats(String, SensorStats)}
     */
    protected void addSensorStats(String key, SensorStats stats) {
        getStats().addSensorStats(key, stats);
    }

    /**
     * Wrapper around {@link SensorStats#addSensorStats(String, SensorStats)} that allows an index
     * to be added. This is useful for {@link SensorOperation}s that have many iterations or child
     * operations. The key added is in the form {@code key + "_" + index} where index may be zero
     * padded.
     */
    protected void addSensorStats(String key, int index, SensorStats stats) {
        addSensorStats(String.format("%s_%03d", key, index), stats);
    }
}
