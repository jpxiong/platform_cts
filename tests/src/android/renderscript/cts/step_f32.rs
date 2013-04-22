#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct step_input {
    float x;
    float y;
};

void root(const struct step_input *in, float *out) {
    *out = step(in->x, in->y);
}
