#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct _set_object_script_input {
    rs_script script;
}set_object_script_input;

void root( const set_object_script_input *in, int *out)
{
    rs_script dst;
    rsSetObject(&dst,in->script);
    *out = ( dst.p == in->script.p ? 1 : 0 );
}
