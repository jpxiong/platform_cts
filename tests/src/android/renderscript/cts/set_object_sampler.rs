#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct _set_object_sampler_input {
    rs_sampler sampler;
}set_object_sampler_input;

void root( const set_object_sampler_input *in, int *out)
{
    rs_sampler dst;
    rsSetObject(&dst,in->sampler);
    *out = ( dst.p == in->sampler.p ? 1 : 0 );
}
