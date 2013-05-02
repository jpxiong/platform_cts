#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct fdim_f32_input {
    float x;
    float y;
};

void root(const struct fdim_f32_input *in, float *out) {
    *out = fdim(in->x, in->y);
}