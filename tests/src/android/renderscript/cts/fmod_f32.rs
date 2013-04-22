#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct fmod_input_f32 {
    float param1;
    float param2;
};

void root(const struct fmod_input_f32 *in, float *out) {
    *out = fmod(in->param1, in->param2);
}