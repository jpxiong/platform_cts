#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct atan2pi_float4_input {
    float4 x;
    float4 y;
};

void root (const struct atan2pi_float4_input* in, float4* out) {
    *out = atan2pi(in->x, in->y);
}
