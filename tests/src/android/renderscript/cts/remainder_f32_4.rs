#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct remainder_f32_4 {
    float4 num;
    float4 den;
};

void root (const struct remainder_f32_4* in, float4* out) {
    *out = remainder(in->num, in->den);
}
