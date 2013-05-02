#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct copysign_f32_2_input {
    float2 x;
    float2 y;
};

void root(const struct copysign_f32_2_input *in, float2 *out) {
    *out = copysign(in->x, in->y);
}
