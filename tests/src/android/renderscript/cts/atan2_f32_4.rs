#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct atan2_f32_4_in {
    float4 first;
    float4 second;
} input;

void root(const input* in, float4* out){
    *out = atan2(in->first, in->second);
}
