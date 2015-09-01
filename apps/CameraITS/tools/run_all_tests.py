# Copyright 2014 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import os
import os.path
import tempfile
import subprocess
import time
import sys
import its.device

def main():
    """Run all the automated tests, saving intermediate files, and producing
    a summary/report of the results.

    Script should be run from the top-level CameraITS directory.
    """

    # Not yet mandated tests
    NOT_YET_MANDATED = {
        "scene0":[
            "test_jitter"
        ],
        "scene1":[
            "test_ae_precapture_trigger",
            "test_black_white",
            "test_burst_sameness_manual",
            "test_crop_region_raw",
            "test_crop_regions",
            "test_exposure",
            "test_locked_burst",
            "test_param_exposure_time",
            "test_param_flash_mode",
            "test_yuv_plus_jpeg",
            "test_yuv_plus_raw",
            "test_yuv_plus_raw10"
        ]
    }

    # Get all the scene0 and scene1 tests, which can be run using the same
    # physical setup.
    scenes = ["scene0", "scene1"]
    tests = []
    for d in scenes:
        tests += [(d,s[:-3],os.path.join("tests", d, s))
                  for s in os.listdir(os.path.join("tests",d))
                  if s[-3:] == ".py"]
    tests.sort()

    # Make output directories to hold the generated files.
    topdir = tempfile.mkdtemp()
    for d in scenes:
        os.mkdir(os.path.join(topdir, d))
    print "Saving output files to:", topdir, "\n"

    # Run each test, capturing stdout and stderr.
    numpass = 0
    numnotmandatedfail = 0
    for (scene,testname,testpath) in tests:
        cmd = ['python', os.path.join(os.getcwd(),testpath)] + sys.argv[1:]
        outdir = os.path.join(topdir,scene)
        outpath = os.path.join(outdir,testname+"_stdout.txt")
        errpath = os.path.join(outdir,testname+"_stderr.txt")
        t0 = time.time()
        with open(outpath,"w") as fout, open(errpath,"w") as ferr:
            retcode = subprocess.call(cmd,stderr=ferr,stdout=fout,cwd=outdir)
        t1 = time.time()
        retstr = "PASS "
        if retcode != 0:
            retstr = "FAIL*" if testname in NOT_YET_MANDATED[scene] else "FAIL "

        if retstr == "FAIL*":
            numnotmandatedfail += 1

        print "%s %s/%s [%.1fs]" % (retstr, scene, testname, t1-t0)
        if retcode == 0 or testname in NOT_YET_MANDATED[scene]:
            numpass += 1

    print "\n%d / %d tests passed (%.1f%%)" % (
            numpass, len(tests), 100.0*float(numpass)/len(tests))
    if numnotmandatedfail > 0:
        print "(*) tests are not yet mandated"
    its.device.ItsSession.report_result(numpass == len(tests))

if __name__ == '__main__':
    main()

