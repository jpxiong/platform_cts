#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct InputData_3 {
    float3 a;
    float3 b;
} InputData_3;

void root(const InputData_3 *in, float3 *out) {
    *out = nextafter (in->a, in->b);
}
