#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct mad_input_f32 {
    float x;
    float y;
    float z;
};

void root(const struct mad_input_f32 *param, float *out) {
    *out = mad(param->x, param->y, param->z);
}
