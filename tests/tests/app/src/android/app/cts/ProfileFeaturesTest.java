/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.app.cts;

import android.annotation.cts.Profile;
import android.annotation.cts.RequiredFeatures;
import android.annotation.cts.SupportedProfiles;
import android.content.pm.PackageManager;
import android.test.AndroidTestCase;

/**
 * Test for checking that devices with different profiles report the correct combination of
 * features mandated by the CDD.
 * <p>
 * It is also currently a demonstration of the {@link SupportedProfiles} and
 * {@link RequiredFeatures} annotations.
 */
public class ProfileFeaturesTest extends AndroidTestCase {

    @SupportedProfiles(Profile.HANDHELD)
    public void testHandheldFeatures() {
        // TODO: Add tests to check that this handheld reports a correct combination of features.
    }

    @SupportedProfiles(Profile.STB)
    public void testStbFeatures() {
        // TODO: Add tests to check that the STB reports a correct combination of features.
    }

    @RequiredFeatures(PackageManager.FEATURE_TELEPHONY_CDMA)
    public void testRequiredFeatures() {
        // This is just a demonstration and compilation test of the RequiredFeatures annotation
        // that can be removed later when the annotation starts being used.
    }
}
