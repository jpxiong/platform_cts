#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct fmax_f32_4_in {
    float4 first;
    float4 second;
} input;

void root(const input* in, float4* out){
    *out = fmax(in->first, in->second);
}
