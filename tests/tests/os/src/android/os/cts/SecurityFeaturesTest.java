/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.os.cts;

import junit.framework.TestCase;

public class SecurityFeaturesTest extends TestCase {

    public void testNoNewPrivs() {
        int newPrivs = OSFeatures.getNoNewPrivs();
        // if newPrivs == -1, then old kernel with no PR_SET_NO_NEW_PRIVS (acceptable)
        // if newPrivs == 0,  then new kernel with PR_SET_NO_NEW_PRIVS disabled (BAD)
        // if newPrivs == 1,  then new kernel with PR_SET_NO_NEW_PRIVS enabled (GOOD)
        assertTrue(newPrivs != 0);
    }
}
