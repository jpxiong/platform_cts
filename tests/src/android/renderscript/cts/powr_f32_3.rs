#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct PowInputData_3 {
    float3 base;
    float3 expo;
} PowInputData_3;

void root(const PowInputData_3 *in, float3 *out) {
    *out = powr(in->base, in->expo);
}
