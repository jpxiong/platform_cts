#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

rs_element element;

void root( int *out)
{
    rsClearObject( &element );
    *out = ( NULL == element.p ? 1 : 0 );
}
