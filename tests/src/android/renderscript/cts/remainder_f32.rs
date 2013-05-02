#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct remainder_f32 {
    float num;
    float den;
};

void root (const struct remainder_f32* in, float* out) {
    *out = remainder(in->num, in->den);
}
