#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct fmod_input_f32_2 {
    float2 param1;
    float2 param2;
};

void root(const struct fmod_input_f32_2 *in, float2 *out) {
    *out = fmod(in->param1, in->param2);
}