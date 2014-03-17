package android.hardware.cts.helpers.sensoroperations;

import android.hardware.cts.helpers.SensorStats;

/**
 * A {@link ISensorOperation} which contains a common implementation for gathering
 * {@link SensorStats}.
 */
public abstract class AbstractSensorOperation implements ISensorOperation {

    private final SensorStats mStats = new SensorStats();

    /**
     * Wrapper around {@link SensorStats#addValue(String, Object)}
     */
    protected void addValue(String key, Object value) {
        mStats.addValue(key, value);
    }

    /**
     * Wrapper around {@link SensorStats#addSensorStats(String, SensorStats)}
     */
    protected void addSensorStats(String key, SensorStats stats) {
        mStats.addSensorStats(key, stats);
    }

    /**
     * Wrapper around {@link SensorStats#addSensorStats(String, SensorStats)} that allows an index
     * to be added. This is useful for {@link ISensorOperation}s that have many iterations or child
     * operations. The key added is in the form {@code key + "_" + index} where index may be zero
     * padded.
     */
    protected void addSensorStats(String key, int index, SensorStats stats) {
        addSensorStats(String.format("%s_%03d", key, index), stats);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SensorStats getStats() {
        return mStats;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract ISensorOperation clone();

}
