#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct PowInputData {
    float base;
    float expo;
} PowInputData;

void root(const PowInputData *in, float *out) {
    *out = pow(in->base, in->expo);
}
