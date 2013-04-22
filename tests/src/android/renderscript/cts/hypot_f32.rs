#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct hypot_f32_in {
    float x;
    float y;
} hypot_input_f32;

void root(const hypot_input_f32 *in, float *out) {
    *out = hypot(in->x, in->y);
}
