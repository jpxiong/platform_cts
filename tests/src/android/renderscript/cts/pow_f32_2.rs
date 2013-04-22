#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct PowInputData_2 {
    float2 base;
    float2 expo;
} PowInputData_2;

void root(const PowInputData_2 *in, float2 *out) {
    *out = pow(in->base, in->expo);
}
