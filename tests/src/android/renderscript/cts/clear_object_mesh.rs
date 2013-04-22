#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

rs_mesh mesh;

void root(int *out)
{
    rsClearObject( &mesh );
    *out = ( NULL == mesh.p ? 1 : 0 );
}
