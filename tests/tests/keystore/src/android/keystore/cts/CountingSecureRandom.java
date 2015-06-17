package android.keystore.cts;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link SecureRandom} which counts how many bytes it has output.
 */
public class CountingSecureRandom extends SecureRandom {

    private final SecureRandom mDelegate = new SecureRandom();
    private final AtomicLong mOutputSizeBytes = new AtomicLong();

    public long getOutputSizeBytes() {
        return mOutputSizeBytes.get();
    }

    public void resetCounters() {
        mOutputSizeBytes.set(0);
    }

    @Override
    public byte[] generateSeed(int numBytes) {
        if (numBytes > 0) {
            mOutputSizeBytes.addAndGet(numBytes);
        }
        return mDelegate.generateSeed(numBytes);
    }

    @Override
    public String getAlgorithm() {
        return mDelegate.getAlgorithm();
    }

    @Override
    public synchronized void nextBytes(byte[] bytes) {
        if ((bytes != null) && (bytes.length > 0)) {
            mOutputSizeBytes.addAndGet(bytes.length);
        }
        mDelegate.nextBytes(bytes);
    }

    @Override
    public synchronized void setSeed(byte[] seed) {
        // Ignore seeding -- not needed in tests and may impact the quality of the output of the
        // delegate SecureRandom by preventing it from self-seeding
    }

    @Override
    public void setSeed(long seed) {
        // Ignore seeding -- not needed in tests and may impact the quality of the output of the
        // delegate SecureRandom by preventing it from self-seeding
    }

    @Override
    public boolean nextBoolean() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double nextDouble() {
        throw new UnsupportedOperationException();
    }

    @Override
    public float nextFloat() {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized double nextGaussian() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int nextInt() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int nextInt(int n) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long nextLong() {
        throw new UnsupportedOperationException();
    }
}
