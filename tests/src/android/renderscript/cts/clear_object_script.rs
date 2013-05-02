#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

rs_script script;

void root( int *out)
{
    rsClearObject( &script );
    *out = ( NULL == script.p ? 1 : 0 );
}
