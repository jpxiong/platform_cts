#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct _object_type_input {
    rs_type type;
}object_type_input;

void root( const object_type_input *in, int *out)
{
    *out = rsIsObject(in->type)==false ? 0 : 1;
}
