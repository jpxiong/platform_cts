#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

void root (const float4* in, float4* out) {
    *out = rint(*in);
}
