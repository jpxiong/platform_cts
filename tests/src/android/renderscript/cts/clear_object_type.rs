#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

rs_type type;

void root(int *out)
{
    rsClearObject( &type );
    *out = ( NULL == type.p ? 1 : 0 );
}
