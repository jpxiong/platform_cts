#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

void root (const float2* in, float2* out) {
    *out = sqrt(*in);
}
