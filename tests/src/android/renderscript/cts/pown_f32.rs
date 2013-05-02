#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

rs_allocation n;

void root(const float *in, float *out, uint32_t x) {
    *out = pown(*in, *(int *)rsGetElementAt(n,x));
}
