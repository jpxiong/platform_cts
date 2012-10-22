#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

int dimX;
int dimY;
int xStart = 0;
int xEnd = 0;
int yStart = 0;
int yEnd = 0;

rs_script s;
rs_allocation ain;
rs_allocation aout;

void run_clipped_script() {
    rs_script_call_t rssc = {0};
    rssc.strategy = RS_FOR_EACH_STRATEGY_DONT_CARE;
    rssc.xStart = xStart;
    rssc.xEnd = xEnd;
    rssc.yStart = yStart;
    rssc.yEnd = yEnd;

    rsForEach(s, ain, aout, NULL, 0, &rssc);

}
