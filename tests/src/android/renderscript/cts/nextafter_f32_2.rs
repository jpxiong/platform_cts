#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct InputData_2 {
    float2 a;
    float2 b;
} InputData_2;

void root(const InputData_2 *in, float2 *out) {
    *out = nextafter (in->a, in->b);
}
