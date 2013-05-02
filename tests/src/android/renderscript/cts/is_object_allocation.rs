#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct _object_allocation_input {
    rs_allocation allocation;
}object_allocation_input;

void root( const object_allocation_input *in, int *out)
{
    *out = rsIsObject(in->allocation)==false ? 0 : 1;
}
