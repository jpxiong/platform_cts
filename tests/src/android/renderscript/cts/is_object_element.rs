#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct _object_element_input {
    rs_element element;
}object_element_input;

void root( const object_element_input *in, int *out)
{
    *out = rsIsObject(in->element)==false ? 0 : 1;
}
