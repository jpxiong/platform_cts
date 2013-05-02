#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct Floats {
    float fa;
    float fb;
    float fc;
} Floats;

void root(const Floats *in, float *out) {
    *out = fma(in->fa, in->fb, in->fc);
}