#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct hypot_f32_2_in {
    float2 x;
    float2 y;
} hypot_input_f32_2;

void root(const hypot_input_f32_2 *in, float2 *out) {
    *out = hypot(in->x, in->y);
}
