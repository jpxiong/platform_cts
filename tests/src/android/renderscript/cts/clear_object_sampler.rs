#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

rs_sampler sampler;

void root(int *out)
{
    rsClearObject( &sampler );
    *out = ( NULL == sampler.p ? 1 : 0 );
}
