#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct mad_input_f32_2 {
    float2 x;
    float2 y;
    float2 z;
};

void root(const struct mad_input_f32_2 *param, float2 *out) {
    *out = mad(param->x, param->y, param->z);
}
