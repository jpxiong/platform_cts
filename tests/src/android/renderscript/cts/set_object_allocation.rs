#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct _set_object_allocation_input {
    rs_allocation allocation;
}set_object_allocation_input;

void root( const set_object_allocation_input *in, int *out)
{
    rs_allocation dst;
    rsSetObject(&dst,in->allocation);
    *out = ( dst.p == in->allocation.p ? 1 : 0 );
}
