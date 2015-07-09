# Copyright 2015 The Android Open Source Project
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
import math
import matplotlib
import matplotlib.pyplot
import os.path
import pylab

def main():
    """Test that the android.noiseReduction.mode param is applied when set for
       reprocessing requests.

    Capture reprocessed images with the camera dimly lit. Uses a high analog
    gain to ensure the captured image is noisy.

    Captures three reprocessed images, for NR off, "fast", and "high quality".
    Also captures a reprocessed image with low gain and NR off, and uses the
    variance of this as the baseline.
    """

    NAME = os.path.basename(__file__).split(".")[0]

    with its.device.ItsSession() as cam:
        props = cam.get_camera_properties()

        its.caps.skip_unless(its.caps.compute_target_exposure(props) and
                             its.caps.per_frame_control(props) and
                             its.caps.noise_reduction_mode(props, 0) and
                             (its.caps.yuv_reprocess(props) or
                              its.caps.private_reprocess(props)))

        # If reprocessing is supported, ZSL NR mode must be avaiable.
        assert(its.caps.noise_reduction_mode(props, 4))

        reprocess_formats = []
        if (its.caps.yuv_reprocess(props)):
            reprocess_formats.append("yuv")
        if (its.caps.private_reprocess(props)):
            reprocess_formats.append("private")

        for reprocess_format in reprocess_formats:
            # List of variances for R, G, B.
            variances = []
            nr_modes_reported = []

            # NR mode 0 with low gain
            e, s = its.target.get_target_exposure_combos(cam)["minSensitivity"]
            req = its.objects.manual_capture_request(s, e)
            req["android.noiseReduction.mode"] = 0

            # Test reprocess_format->JPEG reprocessing
            # TODO: Switch to reprocess_format->YUV when YUV reprocessing is
            #       supported.
            size = its.objects.get_available_output_sizes("jpg", props)[0]
            out_surface = {"width":size[0], "height":size[1], "format":"jpg"}
            cap = cam.do_capture(req, out_surface, reprocess_format)
            img = its.image.decompress_jpeg_to_rgb_image(cap["data"])
            its.image.write_image(img, "%s_low_gain_fmt=jpg.jpg" % (NAME))
            tile = its.image.get_image_patch(img, 0.45, 0.45, 0.1, 0.1)
            ref_variance = its.image.compute_image_variances(tile)
            print "Ref variances:", ref_variance

            for nr_mode in range(5):
                # Skipp unavailable modes
                if not its.caps.noise_reduction_mode(props, nr_mode):
                    nr_modes_reported.append(nr_mode)
                    variances.append(0)
                    continue

                # NR modes with high gain
                e, s = its.target.get_target_exposure_combos(cam) \
                    ["maxSensitivity"]
                req = its.objects.manual_capture_request(s, e)
                req["android.noiseReduction.mode"] = nr_mode
                cap = cam.do_capture(req, out_surface, reprocess_format)
                nr_modes_reported.append(
                    cap["metadata"]["android.noiseReduction.mode"])

                img = its.image.decompress_jpeg_to_rgb_image(cap["data"])
                its.image.write_image(
                    img, "%s_high_gain_nr=%d_fmt=jpg.jpg" % (NAME, nr_mode))
                tile = its.image.get_image_patch(img, 0.45, 0.45, 0.1, 0.1)
                # Get the variances for R, G, and B channels
                variance = its.image.compute_image_variances(tile)
                variances.append(
                    [variance[chan] / ref_variance[chan] for chan in range(3)])
            print "Variances with NR mode [0,1,2,3,4]:", variances

            # Draw a plot.
            for nr_mode in range(5):
                if not its.caps.noise_reduction_mode(props, nr_mode):
                    continue
                pylab.plot(range(3), variances[nr_mode], "rgbcm"[nr_mode])
            matplotlib.pyplot.savefig("%s_plot_%s_variances.png" %
                                      (NAME, reprocess_format))

            assert(nr_modes_reported == [0,1,2,3,4]

            # Check that the variances of the NR=0 and NR=3 and NR=4 images are
            # higher than for the NR=1 and NR=2 images.
            for channel in range(3):
                for nr_mode in [1, 2]:
                    if its.caps.noise_reduction_mode(props, nr_mode):
                        assert(variances[nr_mode][channel] <
                               variances[0][channel])
                        if its.caps.noise_reduction_mode(props, 3):
                            assert(variances[nr_mode][channel] <
                                   variances[3][channel])
                        if its.caps.noise_reduction_mode(props, 4):
                            assert(variances[nr_mode][channel] <
                                   variances[4][channel])

if __name__ == '__main__':
    main()

