#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct fmod_input_f32_3 {
    float3 param1;
    float3 param2;
};

void root(const struct fmod_input_f32_3 *in, float3 *out) {
    *out = fmod(in->param1, in->param2);
}