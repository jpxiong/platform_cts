#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct copysign_f32_input {
    float x;
    float y;
};

void root(const struct copysign_f32_input *in, float *out) {
    *out = copysign(in->x, in->y);
}
