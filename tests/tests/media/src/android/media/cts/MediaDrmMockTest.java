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

package android.media.cts;

import android.media.MediaDrm;
import android.media.MediaDrm.ProvisionRequest;
import android.media.MediaDrm.LicenseRequest;
import android.media.MediaDrmException;
import android.test.AndroidTestCase;
import android.util.Log;
import java.util.HashMap;
import java.util.Arrays;
import java.util.UUID;

// This test works with the MediaDrm mock plugin
public class MediaDrmMockTest extends AndroidTestCase {
    private static final String TAG = "MediaDrmMockTest";

    // The scheme supported by the mock drm plugin
    static final UUID mockScheme = new UUID(0x0102030405060708L, 0x090a0b0c0d0e0f10L);
    static final UUID badScheme = new UUID(0xffffffffffffffffL, 0xffffffffffffffffL);

    private boolean isMockPluginInstalled() {
        return MediaDrm.isCryptoSchemeSupported(mockScheme);
    }

    public void testIsCryptoSchemeNotSupported() throws Exception {
        assertFalse(MediaDrm.isCryptoSchemeSupported(badScheme));
    }

    public void testMediaDrmConstructor() throws Exception {
        if (isMockPluginInstalled()) {
            MediaDrm md = new MediaDrm(mockScheme);
        } else {
            Log.w(TAG, "optional plugin libmockdrmcryptoplugin.so is not installed");
            Log.w(TAG, "To verify the MediaDrm APIs, you should install this plugin");
        }
    }

    public void testMediaDrmConstructorFails() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        boolean gotException = false;
        try {
            MediaDrm md = new MediaDrm(badScheme);
        } catch (MediaDrmException e) {
            gotException = true;
        }
        assertTrue(gotException);
    }

    public void testStringProperties() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);

        md.setPropertyString("test-string", "test-value");
        assertTrue(md.getPropertyString("test-string").equals("test-value")); 
    }

    public void testByteArrayProperties() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);

        byte testArray[] = {0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x10, 0x11, 0x12};
        md.setPropertyByteArray("test-array", testArray);
        assertTrue(Arrays.equals(md.getPropertyByteArray("test-array"), testArray));
    }

    public void testMissingPropertyString() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);

        boolean gotException = false;
        try {
            md.getPropertyString("missing-property");
        } catch (IllegalArgumentException e) {
            gotException = true;
        }
        assertTrue(gotException);
    }

    public void testNullPropertyString() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);

        boolean gotException = false;
        try {
            md.getPropertyString(null);
        } catch (IllegalArgumentException e) {
            gotException = true;
        }
        assertTrue(gotException);
    }

    public void testMissingPropertyByteArray() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);

        boolean gotException = false;
        try {
            md.getPropertyByteArray("missing-property");
        } catch (IllegalArgumentException e) {
            gotException = true;
        }
        assertTrue(gotException);
    }

    public void testNullPropertyByteArray() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);

        boolean gotException = false;
        try {
            md.getPropertyByteArray(null);
        } catch (IllegalArgumentException e) {
            gotException = true;
        }
        assertTrue(gotException);
    }

    public void testOpenCloseSession() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);
        byte[] sessionId = md.openSession();
        md.closeSession(sessionId);
    }

    public void testBadSession() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);
        byte[] sessionId = {0x05, 0x6, 0x7, 0x8};
        boolean gotException = false;
        try {
            md.closeSession(sessionId);
        } catch (IllegalArgumentException e) {
            gotException = true;
        }
        assertTrue(gotException);
    }

    public void testNullSession() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);
        byte[] sessionId = null;
        boolean gotException = false;
        try {
            md.closeSession(sessionId);
        } catch (IllegalArgumentException e) {
            gotException = true;
        }
        assertTrue(gotException);
    }

    public void testGetLicenseRequest() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);
        byte[] sessionId = md.openSession();

        // Set up mock expected responses using properties
        byte testRequest[] = {0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x10, 0x11, 0x12};
        md.setPropertyByteArray("mock-request", testRequest);
        String testDefaultUrl = "http://1.2.3.4:8080/blah";
        md.setPropertyString("mock-defaultUrl", testDefaultUrl);

        byte[] initData = {0x0a, 0x0b, 0x0c, 0x0d};
        HashMap<String, String> optionalParameters = new HashMap<String, String>();
        optionalParameters.put("param1", "value1");
        optionalParameters.put("param2", "value2");

        String mimeType = "video/iso.segment";
        LicenseRequest request = md.getLicenseRequest(sessionId, initData, mimeType,
                                                      MediaDrm.MEDIA_DRM_LICENSE_TYPE_STREAMING,
                                                      optionalParameters);
        assertTrue(Arrays.equals(request.data, testRequest));
        assertTrue(request.defaultUrl.equals(testDefaultUrl));

        assertTrue(Arrays.equals(initData, md.getPropertyByteArray("mock-initdata")));
        assertTrue(mimeType.equals(md.getPropertyString("mock-mimetype")));
        assertTrue(md.getPropertyString("mock-licensetype").equals("1"));
        assertTrue(md.getPropertyString("mock-optparams").equals("{param1,value1},{param2,value2}"));

        md.closeSession(sessionId);
    }

    public void testGetLicenseRequestNoOptionalParameters() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);
        byte[] sessionId = md.openSession();

        // Set up mock expected responses using properties
        byte testRequest[] = {0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x10, 0x11, 0x12};
        md.setPropertyByteArray("mock-request", testRequest);
        String testDefaultUrl = "http://1.2.3.4:8080/blah";
        md.setPropertyString("mock-defaultUrl", testDefaultUrl);

        byte[] initData = {0x0a, 0x0b, 0x0c, 0x0d};

        String mimeType = "video/iso.segment";
        LicenseRequest request = md.getLicenseRequest(sessionId, initData, mimeType,
                                                      MediaDrm.MEDIA_DRM_LICENSE_TYPE_STREAMING,
                                                      null);
        assertTrue(Arrays.equals(request.data, testRequest));
        assertTrue(request.defaultUrl.equals(testDefaultUrl));

        assertTrue(Arrays.equals(initData, md.getPropertyByteArray("mock-initdata")));
        assertTrue(mimeType.equals(md.getPropertyString("mock-mimetype")));
        assertTrue(md.getPropertyString("mock-licensetype").equals("1"));

        md.closeSession(sessionId);
    }

    public void testProvideLicenseResponse() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);
        byte[] sessionId = md.openSession();

        // Set up mock expected responses using properties
        byte testResponse[] = {0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x20};

        md.provideLicenseResponse(sessionId, testResponse);

        assertTrue(Arrays.equals(testResponse, md.getPropertyByteArray("mock-response")));
        md.closeSession(sessionId);
    }

    public void testRemoveLicense() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);
        byte[] sessionId = md.openSession();
        md.removeLicense(sessionId);
        md.closeSession(sessionId);
    }

    public void testQueryLicenseStatus() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);
        byte[] sessionId = md.openSession();
        HashMap<String, String> infoMap = md.queryLicenseStatus(sessionId);

        // these are canned strings returned by the mock
        assertTrue(infoMap.containsKey("purchaseDuration"));
        assertTrue(infoMap.get("purchaseDuration").equals(("1000")));
        assertTrue(infoMap.containsKey("licenseDuration"));
        assertTrue(infoMap.get("licenseDuration").equals(("100")));

        md.closeSession(sessionId);
    }

    public void testGetProvisionRequest() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);

        // Set up mock expected responses using properties
        byte testRequest[] = {0x5a, 0x5b, 0x5c, 0x5d, 0x5e, 0x60, 0x61, 0x62};
        md.setPropertyByteArray("mock-request", testRequest);
        String testDefaultUrl = "http://1.2.3.4:8080/bar";
        md.setPropertyString("mock-defaultUrl", testDefaultUrl);

        ProvisionRequest request = md.getProvisionRequest();
        assertTrue(Arrays.equals(request.data, testRequest));
        assertTrue(request.defaultUrl.equals(testDefaultUrl));
    }

    public void testProvideProvisionResponse() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);

        // Set up mock expected responses using properties
        byte testResponse[] = {0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x20};

        md.provideProvisionResponse(testResponse);
        assertTrue(Arrays.equals(testResponse, md.getPropertyByteArray("mock-response")));
    }

    public void testMultipleSessions() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);

        byte[] session1 = md.openSession();
        byte[] session2 = md.openSession();
        byte[] session3 = md.openSession();

        assertFalse(Arrays.equals(session1, session2));
        assertFalse(Arrays.equals(session2, session3));

        md.closeSession(session1);
        md.closeSession(session2);
        md.closeSession(session3);
    }

}
