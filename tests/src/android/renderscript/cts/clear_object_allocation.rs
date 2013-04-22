#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

rs_allocation allocation;

void root( int *out)
{
    rsClearObject( &allocation );
    *out = ( NULL == allocation.p ? 1 : 0 );
}
