#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

rs_allocation n;

void root(const float2 *in, float2 *out, uint32_t x) {
    *out = rootn(*in, *(int2 *)rsGetElementAt(n,x));
}
