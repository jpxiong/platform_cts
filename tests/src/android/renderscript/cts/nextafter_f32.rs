#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct InputData {
    float a;
    float b;
} InputData;

void root(const InputData *in, float *out) {
    *out = nextafter (in->a, in->b);
}
