#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct Floats3 {
    float3 fa;
    float3 fb;
    float3 fc;
} Floats3;

void root(const Floats3 *in, float3 *out) {
    *out = fma(in->fa, in->fb, in->fc);
}