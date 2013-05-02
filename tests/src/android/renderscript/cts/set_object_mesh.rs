#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct _set_object_mesh_input {
    rs_mesh mesh;
}set_object_mesh_input;

void root( const set_object_mesh_input *in, int *out)
{
    rs_mesh dst;
    rsSetObject(&dst,in->mesh);
    *out = ( dst.p == in->mesh.p ? 1 : 0 );
}
