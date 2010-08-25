package android.os.cts;

public class CpuFeatures {

    public static final String ARMEABI_V7 = "armeabi-v7a";

    public static final String ARMEABI = "armeabi";

    static {
        System.loadLibrary("cts_jni");
    }

    public static native boolean isArmCpu();

    public static native boolean isArm7Compatible();
}
