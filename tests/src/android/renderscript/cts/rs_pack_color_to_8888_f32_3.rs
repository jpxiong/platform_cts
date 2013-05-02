#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

void root (const float3* in, uchar4* out) {
    *out = rsPackColorTo8888(*in);
}
