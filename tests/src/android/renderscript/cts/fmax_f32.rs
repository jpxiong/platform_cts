#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct fmax_f32_in {
    float first;
    float second;
} input;

void root(const input* in, float* out){
    *out = fmax(in->first, in->second);
}
