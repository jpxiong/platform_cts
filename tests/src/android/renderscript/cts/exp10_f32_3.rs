#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

void root(const float3 *in, float3 *out) {
    *out = exp10(*in);
}
