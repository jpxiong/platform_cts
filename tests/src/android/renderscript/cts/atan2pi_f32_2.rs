#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct atan2pi_float2_input {
    float2 x;
    float2 y;
};

void root (const struct atan2pi_float2_input* in, float2* out) {
    *out = atan2pi(in->x, in->y);
}
