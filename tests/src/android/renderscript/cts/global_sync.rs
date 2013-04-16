#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

int gInt;
static int sInt;

rs_allocation aFailed;

void test_global(int expected) {
    if (gInt != expected) {
        rsSetElementAt_uchar(aFailed, 1, 0);
    }
}

void test_static_global(int expected) {
    if (sInt != expected) {
        rsSetElementAt_uchar(aFailed, 1, 0);
    }
}

void __attribute__((kernel)) write_global(int ain, uint32_t x) {
    if (x == 0) {
        gInt = ain;
    }
}

void __attribute__((kernel)) write_static_global(int ain, uint32_t x) {
    if (x == 0) {
        sInt = ain;
    }
}

