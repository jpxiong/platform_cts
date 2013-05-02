#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct _object_sampler_input {
    rs_sampler sampler;
}object_sampler_input;

void root( const object_sampler_input *in, int *out)
{
    *out = rsIsObject(in->sampler)==false ? 0 : 1;
}
