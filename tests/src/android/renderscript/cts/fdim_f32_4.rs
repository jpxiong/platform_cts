#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct fdim_f32_4_input {
    float4 x;
    float4 y;
};

void root(const struct fdim_f32_4_input *in, float4 *out) {
    *out = fdim(in->x, in->y);
}