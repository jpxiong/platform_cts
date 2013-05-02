#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct hypot_f32_3_in {
    float3 x;
    float3 y;
} hypot_input_f32_3;

void root(const hypot_input_f32_3 *in, float3 *out) {
    *out = hypot(in->x, in->y);
}
