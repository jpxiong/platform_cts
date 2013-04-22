#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct fmin_f32_in {
    float first;
    float second;
} input;

void root(const input* in, float* out){
    *out = fmin(in->first, in->second);
}
