#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct fmod_input_f32_4 {
    float4 param1;
    float4 param2;
};

void root(const struct fmod_input_f32_4 *in, float4 *out) {
    *out = fmod(in->param1, in->param2);
}