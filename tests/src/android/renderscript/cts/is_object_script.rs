#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct _object_script_input {
    rs_script script;
}object_script_input;

void root( const object_script_input *in, int *out)
{
    *out = rsIsObject(in->script)==false ? 0 : 1;
}
