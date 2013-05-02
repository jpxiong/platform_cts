#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

rs_allocation n;

void root(const float4 *in, float4 *out, uint32_t x) {
    *out = pown(*in, *(int4 *)rsGetElementAt(n,x));
}
