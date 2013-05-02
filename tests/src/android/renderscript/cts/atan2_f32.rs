#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct atan2_f32_in {
    float first;
    float second;
} input;

void root(const input* in, float* out){
    *out = atan2(in->first, in->second);
}
