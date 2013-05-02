#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct fdim_f32_3_input {
    float3 x;
    float3 y;
};

void root(const struct fdim_f32_3_input *in, float3 *out) {
    *out = fdim(in->x, in->y);
}