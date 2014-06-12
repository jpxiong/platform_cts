#pragma version(1)
#pragma rs java_package_name(com.android.cts.graphicshardware)

int REGION_SIZE;
int WIDTH;
int HEIGHT;

rs_allocation ideal;
rs_allocation given;

void displayDifference(const uchar4 *v_in, uchar4 *v_out, uint32_t x, uint32_t y) {
    float4 idealPixel = rsGetElementAt_float4(ideal, x, y);
    float4 givenPixel = rsGetElementAt_float4(given, x, y);

    float4 diff = idealPixel - givenPixel;
    float totalDiff = diff.x + diff.y + diff.z + diff.w;
    if (totalDiff < 0) {
        v_out[0] = rsPackColorTo8888(0, 0, clamp(-totalDiff/2.f, 0.f, 1.f));
    } else {
        v_out[0] = rsPackColorTo8888(clamp(totalDiff/2.f, 0.f, 1.f), 0, 0);
    }
}
