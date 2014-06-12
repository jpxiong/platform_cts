#pragma version(1)
#pragma rs java_package_name(com.android.cts.uirendering)

int REGION_SIZE;
int WIDTH;
int HEIGHT;

rs_allocation ideal;
rs_allocation given;

// This method does a threshold comparison of the values
void calcMSE(const int32_t *v_in, float *v_out){
    int y = v_in[0];
    v_out[0] = 0;

    for(int i = 0 ; i < WIDTH ; i ++){
        uchar4 idealPixel = rsGetElementAt_uchar4(ideal, i, y);
        uchar4 givenPixel = rsGetElementAt_uchar4(given, i, y);
        uchar4 diff = idealPixel - givenPixel;
        int totalDiff = abs(diff.x) + abs(diff.y) + abs(diff.z);
        v_out[0] += totalDiff;
    }
}
