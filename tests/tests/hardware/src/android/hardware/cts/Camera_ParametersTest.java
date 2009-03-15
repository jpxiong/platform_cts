/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.hardware.cts;

import junit.framework.TestCase;
import android.hardware.Camera.Parameters;
import dalvik.annotation.TestInfo;
import dalvik.annotation.TestStatus;
import dalvik.annotation.TestTarget;
import dalvik.annotation.TestTargetClass;

@TestTargetClass(Parameters.class)
public class Camera_ParametersTest extends TestCase {

    @TestInfo(
      status = TestStatus.TBR,
      notes = "We test Camera.Parameters related methods in CameraTest with testAccessParameters()",
      targets = {
        @TestTarget(
          methodName = "get",
          methodArgs = {String.class}
        ),
        @TestTarget(
          methodName = "set",
          methodArgs = {String.class, int.class}
        ),
        @TestTarget(
          methodName = "getInt",
          methodArgs = {String.class}
        ),
        @TestTarget(
          methodName = "set",
          methodArgs = {String.class, String.class}
        ),
        @TestTarget(
          methodName = "getPictureFormat",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "setPictureFormat",
          methodArgs = {int.class}
        ),
        @TestTarget(
          methodName = "getPictureSize",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "setPictureSize",
          methodArgs = {int.class, int.class}
        ),
        @TestTarget(
          methodName = "getPreviewFormat",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "setPreviewFormat",
          methodArgs = {int.class}
        ),
        @TestTarget(
          methodName = "getPreviewFrameRate",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "setPreviewFrameRate",
          methodArgs = {int.class}
        ),
        @TestTarget(
          methodName = "getPreviewSize",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "setPreviewSize",
          methodArgs = {int.class, int.class}
        ),
        @TestTarget(
          methodName = "flatten",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "unflatten",
          methodArgs = {String.class}
        ),
        @TestTarget(
          methodName = "remove",
          methodArgs = {String.class}
        )
    })
    public void testAccessMethods() {
        // We test Camera.Parameters related methods in android.hardware.cts.CameraTest
        // #testAccessParameters().
    }

}

