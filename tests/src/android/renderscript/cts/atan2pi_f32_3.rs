#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct atan2pi_float3_input {
    float3 x;
    float3 y;
};

void root (const struct atan2pi_float3_input* in, float3* out) {
    *out = atan2pi(in->x, in->y);
}
