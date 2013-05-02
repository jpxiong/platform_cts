#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct step_4_input {
    float4 x;
    float4 y;
};

void root(const struct step_4_input *in, float4 *out) {
    *out = step(in->x, in->y);
}
