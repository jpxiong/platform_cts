#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct Floats4 {
    float4 fa;
    float4 fb;
    float4 fc;
} Floats4;

void root(const Floats4 *in, float4 *out) {
    *out = fma(in->fa, in->fb, in->fc);
}