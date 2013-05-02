#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)
typedef struct _cross_f32_3_struct {
    float3 low;
    float3 high;
}cross_f32_3_struct;

void root(const cross_f32_3_struct *in, float3 *out) {
    *out = cross(in->low, in->high);
}
