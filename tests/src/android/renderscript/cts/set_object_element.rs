#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct _set_object_element_input {
    rs_element element;
}set_object_element_input;

void root( const set_object_element_input *in, int *out)
{
    rs_element dst;
    rsSetObject(&dst,in->element);
    *out = ( dst.p == in->element.p ? 1 : 0 );
}
