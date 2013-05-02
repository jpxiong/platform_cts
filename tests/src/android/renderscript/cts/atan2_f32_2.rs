#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct atan2_f32_2_in {
    float2 first;
    float2 second;
} input;

void root(const input* in, float2* out){
    *out = atan2(in->first, in->second);
}
