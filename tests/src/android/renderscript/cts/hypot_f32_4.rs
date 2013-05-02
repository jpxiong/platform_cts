#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct hypot_f32_4_in {
    float4 x;
    float4 y;
} hypot_input_f32_4;

void root(const hypot_input_f32_4 *in, float4 *out) {
    *out = hypot(in->x, in->y);
}
