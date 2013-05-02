#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct _set_object_type_input {
    rs_type type;
}set_object_type_input;

void root( const set_object_type_input *in, int *out)
{
    rs_type dst;
    rsSetObject(&dst,in->type);
    *out = ( dst.p == in->type.p ? 1 : 0 );
}
