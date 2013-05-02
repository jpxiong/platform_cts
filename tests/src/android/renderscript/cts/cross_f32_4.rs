#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)
typedef struct _cross_f32_4_struct {
    float4 low;
    float4 high;
}cross_f32_4_struct;

void root(const cross_f32_4_struct *in, float4 *out) {
    *out = cross(in->low, in->high);
}
