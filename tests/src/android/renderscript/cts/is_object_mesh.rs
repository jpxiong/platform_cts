#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct _object_mesh_input {
    rs_mesh mesh;
}object_mesh_input;

void root( const object_mesh_input *in, int *out)
{
    *out = rsIsObject(in->mesh)==false ? 0 : 1;
}
