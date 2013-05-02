#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct remainder_f32_3 {
    float3 num;
    float3 den;
};

void root (const struct remainder_f32_3* in, float3* out) {
    *out = remainder(in->num, in->den);
}
