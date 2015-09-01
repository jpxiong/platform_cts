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

import its.image
import its.caps
import its.device
import its.objects
import its.target
import numpy
import os.path


def check_crop_region(expected, reported, active, err_threshold):
    """Check if the reported region is within the tolerance.

    Args:
        expected: expected crop region
        reported: reported crop region
        active: active resolution
        err_threshold: error threshold for the active resolution
    """

    ex = (active["right"] - active["left"]) * err_threshold
    ey = (active["bottom"] - active["top"]) * err_threshold

    assert ((abs(expected["left"] - reported["left"]) <= ex) and
            (abs(expected["right"] - reported["right"]) <= ex) and
            (abs(expected["top"] - reported["top"]) <= ey) and
            (abs(expected["bottom"] - reported["bottom"]) <= ey))

def main():
    """Test that raw streams are not croppable.
    """
    NAME = os.path.basename(__file__).split(".")[0]

    DIFF_THRESH = 0.05
    CROP_REGION_ERROR_THRESHOLD = 0.01

    with its.device.ItsSession() as cam:
        props = cam.get_camera_properties()
        if (not its.caps.compute_target_exposure(props) or
            not its.caps.raw16(props)):
            print "Test skipped"
            return

        a = props['android.sensor.info.activeArraySize']
        ax, ay = a["left"], a["top"]
        aw, ah = a["right"] - a["left"], a["bottom"] - a["top"]
        print "Active sensor region: (%d,%d %dx%d)" % (ax, ay, aw, ah)

        full_region = {
            "left": 0,
            "top": 0,
            "right": aw,
            "bottom": ah
        }

        # Capture without a crop region.
        # Use a manual request with a linear tonemap so that the YUV and RAW
        # should look the same (once converted by the its.image module).
        e, s = its.target.get_target_exposure_combos(cam)["minSensitivity"]
        req = its.objects.manual_capture_request(s,e, True)
        cap1_raw, cap1_yuv = cam.do_capture(req, cam.CAP_RAW_YUV)

        # Calculate a center crop region.
        zoom = min(3.0, its.objects.get_max_digital_zoom(props))
        assert(zoom >= 1)
        cropw = aw / zoom
        croph = ah / zoom

        req["android.scaler.cropRegion"] = {
            "left": aw / 2 - cropw / 2,
            "top": ah / 2 - croph / 2,
            "right": aw / 2 + cropw / 2,
            "bottom": ah / 2 + croph / 2
        }

        # when both YUV and RAW are requested, the crop region that's
        # applied to YUV should be reported.
        crop_region = req["android.scaler.cropRegion"]
        if crop_region == full_region:
            crop_region_err_thresh = 0.0
        else:
            crop_region_err_thresh = CROP_REGION_ERROR_THRESHOLD

        cap2_raw, cap2_yuv = cam.do_capture(req, cam.CAP_RAW_YUV)

        imgs = {}
        for s, cap, cr, err_delta in [("yuv_full", cap1_yuv, full_region, 0),
                      ("raw_full", cap1_raw, full_region, 0),
                      ("yuv_crop", cap2_yuv, crop_region, crop_region_err_thresh),
                      ("raw_crop", cap2_raw, crop_region, crop_region_err_thresh)]:
            img = its.image.convert_capture_to_rgb_image(cap, props=props)
            its.image.write_image(img, "%s_%s.jpg" % (NAME, s))
            r = cap["metadata"]["android.scaler.cropRegion"]
            x, y = r["left"], r["top"]
            w, h = r["right"] - r["left"], r["bottom"] - r["top"]
            imgs[s] = img
            print "Crop on %s: (%d,%d %dx%d)" % (s, x, y, w, h)
            check_crop_region(cr, r, a, err_delta)

        # Also check the image content; 3 of the 4 shots should match.
        # Note that all the shots are RGB below; the variable names correspond
        # to what was captured.
        # Average the images down 4x4 -> 1 prior to comparison to smooth out
        # noise.
        # Shrink the YUV images an additional 2x2 -> 1 to account for the size
        # reduction that the raw images went through in the RGB conversion.
        imgs2 = {}
        for s,img in imgs.iteritems():
            h,w,ch = img.shape
            m = 4
            if s in ["yuv_full", "yuv_crop"]:
                m = 8
            img = img.reshape(h/m,m,w/m,m,3).mean(3).mean(1).reshape(h/m,w/m,3)
            imgs2[s] = img
            print s, img.shape

        # Strip any border pixels from the raw shots (since the raw images may
        # be larger than the YUV images). Assume a symmetric padded border.
        xpad = (imgs2["raw_full"].shape[1] - imgs2["yuv_full"].shape[1]) / 2
        ypad = (imgs2["raw_full"].shape[0] - imgs2["yuv_full"].shape[0]) / 2
        wyuv = imgs2["yuv_full"].shape[1]
        hyuv = imgs2["yuv_full"].shape[0]
        imgs2["raw_full"]=imgs2["raw_full"][ypad:ypad+hyuv:,xpad:xpad+wyuv:,::]
        imgs2["raw_crop"]=imgs2["raw_crop"][ypad:ypad+hyuv:,xpad:xpad+wyuv:,::]
        print "Stripping padding before comparison:", xpad, ypad

        for s,img in imgs2.iteritems():
            its.image.write_image(img, "%s_comp_%s.jpg" % (NAME, s))

        # Compute image diffs.
        diff_yuv = numpy.fabs((imgs2["yuv_full"] - imgs2["yuv_crop"])).mean()
        diff_raw = numpy.fabs((imgs2["raw_full"] - imgs2["raw_crop"])).mean()
        print "YUV diff (crop vs. non-crop):", diff_yuv
        print "RAW diff (crop vs. non-crop):", diff_raw

        assert(diff_yuv > DIFF_THRESH)
        assert(diff_raw < DIFF_THRESH)

if __name__ == '__main__':
    main()

