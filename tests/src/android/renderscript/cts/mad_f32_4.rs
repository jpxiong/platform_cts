#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct mad_input_f32_4 {
    float4 x;
    float4 y;
    float4 z;
};

void root(const struct mad_input_f32_4 *param, float4 *out) {
    *out = mad(param->x, param->y, param->z);
}
