#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct PowInputData_4 {
    float4 base;
    float4 expo;
} PowInputData_4;

void root(const PowInputData_4 *in, float4 *out) {
    *out = pow(in->base, in->expo);
}
