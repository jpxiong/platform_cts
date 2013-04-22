#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct atan2pi_float_input {
    float x;
    float y;
};

void root (const struct atan2pi_float_input* in, float* out) {
    *out = atan2pi(in->x, in->y);
}
