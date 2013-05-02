#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct InputData_4 {
    float4 a;
    float4 b;
} InputData_4;

void root(const InputData_4 *in, float4 *out) {
    *out = nextafter (in->a, in->b);
}
