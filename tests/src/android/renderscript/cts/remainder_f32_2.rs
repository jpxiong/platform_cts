#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct remainder_f32_2 {
    float2 num;
    float2 den;
};

void root (const struct remainder_f32_2* in, float2* out) {
    *out = remainder(in->num, in->den);
}
