#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct step_2_input {
    float2 x;
    float2 y;
};

void root(const struct step_2_input *in, float2 *out) {
    *out = step(in->x, in->y);
}
