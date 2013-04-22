#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct Floats2 {
    float2 fa;
    float2 fb;
    float2 fc;
} Floats2;

void root(const Floats2 *in, float2 *out) {
    *out = fma(in->fa, in->fb, in->fc);
}