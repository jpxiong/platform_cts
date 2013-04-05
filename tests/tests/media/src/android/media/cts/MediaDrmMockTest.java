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
import android.media.MediaDrm.KeyRequest;
import android.media.MediaDrm.CryptoSession;
import android.media.MediaDrmException;
import android.test.AndroidTestCase;
import android.util.Log;
import java.util.HashMap;
import java.util.Arrays;
import java.util.UUID;
import java.lang.Thread;
import java.lang.Object;
import android.os.Looper;

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

    public void testGetKeyRequest() throws Exception {
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
        KeyRequest request = md.getKeyRequest(sessionId, initData, mimeType,
                                                      MediaDrm.MEDIA_DRM_KEY_TYPE_STREAMING,
                                                      optionalParameters);
        assertTrue(Arrays.equals(request.data, testRequest));
        assertTrue(request.defaultUrl.equals(testDefaultUrl));

        assertTrue(Arrays.equals(initData, md.getPropertyByteArray("mock-initdata")));
        assertTrue(mimeType.equals(md.getPropertyString("mock-mimetype")));
        assertTrue(md.getPropertyString("mock-keytype").equals("1"));
        assertTrue(md.getPropertyString("mock-optparams").equals("{param1,value1},{param2,value2}"));

        md.closeSession(sessionId);
    }

    public void testGetKeyRequestNoOptionalParameters() throws Exception {
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
        KeyRequest request = md.getKeyRequest(sessionId, initData, mimeType,
                                                      MediaDrm.MEDIA_DRM_KEY_TYPE_STREAMING,
                                                      null);
        assertTrue(Arrays.equals(request.data, testRequest));
        assertTrue(request.defaultUrl.equals(testDefaultUrl));

        assertTrue(Arrays.equals(initData, md.getPropertyByteArray("mock-initdata")));
        assertTrue(mimeType.equals(md.getPropertyString("mock-mimetype")));
        assertTrue(md.getPropertyString("mock-keytype").equals("1"));

        md.closeSession(sessionId);
    }

    public void testProvideKeyResponse() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);
        byte[] sessionId = md.openSession();

        // Set up mock expected responses using properties
        byte testResponse[] = {0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x20};

        md.provideKeyResponse(sessionId, testResponse);

        assertTrue(Arrays.equals(testResponse, md.getPropertyByteArray("mock-response")));
        md.closeSession(sessionId);
    }

    public void testRemoveKeys() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);
        byte[] sessionId = md.openSession();

        byte testResponse[] = {0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x20};
        byte[] keySetId = md.provideKeyResponse(sessionId, testResponse);
        md.closeSession(sessionId);

        md.removeKeys(keySetId);
    }

    public void testRestoreKeys() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);
        byte[] sessionId = md.openSession();

        byte testResponse[] = {0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x20};
        byte[] keySetId = md.provideKeyResponse(sessionId, testResponse);
        md.closeSession(sessionId);

        sessionId = md.openSession();
        md.restoreKeys(sessionId, keySetId);
        md.closeSession(sessionId);
    }

    public void testQueryKeyStatus() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);
        byte[] sessionId = md.openSession();
        HashMap<String, String> infoMap = md.queryKeyStatus(sessionId);

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

    public void testCryptoSession() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);

        byte[] sessionId = md.openSession();
        CryptoSession cs = md.getCryptoSession(sessionId, "AES/CBC/NoPadding", "HmacSHA256");
        assertFalse(cs == null);
    }

    public void testBadCryptoSession() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);

        boolean gotException = false;
        try {
            byte[] sessionId = md.openSession();
            CryptoSession cs = md.getCryptoSession(sessionId, "bad", "bad");
        } catch (IllegalArgumentException e) {
            gotException = true;
        }
        assertTrue(gotException);
    }

    public void testCryptoSessionEncrypt() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);

        byte[] sessionId = md.openSession();
        CryptoSession cs = md.getCryptoSession(sessionId, "AES/CBC/NoPadding", "HmacSHA256");
        assertFalse(cs == null);

        byte[] keyId = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09};
        byte[] input = {0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19};
        byte[] iv = {0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29};
        byte[] expected_output = {0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39};

        md.setPropertyByteArray("mock-output", expected_output);

        byte[] output = cs.encrypt(keyId, input, iv);

        assertTrue(Arrays.equals(keyId, md.getPropertyByteArray("mock-keyid")));
        assertTrue(Arrays.equals(input, md.getPropertyByteArray("mock-input")));
        assertTrue(Arrays.equals(iv, md.getPropertyByteArray("mock-iv")));
        assertTrue(Arrays.equals(output, expected_output));
    }

    public void testCryptoSessionDecrypt() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);

        byte[] sessionId = md.openSession();
        CryptoSession cs = md.getCryptoSession(sessionId, "AES/CBC/NoPadding", "HmacSHA256");
        assertFalse(cs == null);

        byte[] keyId = {0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49};
        byte[] input = {0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59};
        byte[] iv = {0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69};
        byte[] expected_output = {0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79};

        md.setPropertyByteArray("mock-output", expected_output);

        byte[] output = cs.decrypt(keyId, input, iv);

        assertTrue(Arrays.equals(keyId, md.getPropertyByteArray("mock-keyid")));
        assertTrue(Arrays.equals(input, md.getPropertyByteArray("mock-input")));
        assertTrue(Arrays.equals(iv, md.getPropertyByteArray("mock-iv")));
        assertTrue(Arrays.equals(output, expected_output));
    }

    public void testCryptoSessionSign() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);

        byte[] sessionId = md.openSession();
        CryptoSession cs = md.getCryptoSession(sessionId, "AES/CBC/NoPadding", "HmacSHA256");
        assertFalse(cs == null);

        byte[] keyId = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09};
        byte[] message = {0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29};
        byte[] expected_signature = {0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39};

        md.setPropertyByteArray("mock-signature", expected_signature);

        byte[] signature = cs.sign(keyId, message);

        assertTrue(Arrays.equals(keyId, md.getPropertyByteArray("mock-keyid")));
        assertTrue(Arrays.equals(message, md.getPropertyByteArray("mock-message")));
        assertTrue(Arrays.equals(signature, expected_signature));
    }

    public void testCryptoSessionVerify() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }

        MediaDrm md = new MediaDrm(mockScheme);

        byte[] sessionId = md.openSession();
        CryptoSession cs = md.getCryptoSession(sessionId, "AES/CBC/NoPadding", "HmacSHA256");
        assertFalse(cs == null);

        byte[] keyId = {0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49};
        byte[] message = {0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59};
        byte[] signature = {0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69};

        md.setPropertyString("mock-match", "1");
        assertTrue(cs.verify(keyId, message, signature));

        assertTrue(Arrays.equals(keyId, md.getPropertyByteArray("mock-keyid")));
        assertTrue(Arrays.equals(message, md.getPropertyByteArray("mock-message")));
        assertTrue(Arrays.equals(signature, md.getPropertyByteArray("mock-signature")));

        md.setPropertyString("mock-match", "0");
        assertFalse(cs.verify(keyId, message, signature));
    }

    private MediaDrm mMediaDrm = null;
    private Looper mLooper = null;
    private Object mLock = new Object();
    private boolean mGotEvent = false;

    public void testEventNoSessionNoData() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }


        new Thread() {
            @Override
            public void run() {
                // Set up a looper to be used by mMediaPlayer.
                Looper.prepare();

                // Save the looper so that we can terminate this thread
                // after we are done with it.
                mLooper = Looper.myLooper();

                try {
                    mMediaDrm = new MediaDrm(mockScheme);

                    synchronized(mLock) {
                        mLock.notify();
                        mMediaDrm.setOnEventListener(new MediaDrm.OnEventListener() {
                                @Override
                                public void onEvent(MediaDrm md, byte[] sessionId, int event,
                                                    int extra, byte[] data) {
                                    synchronized(mLock) {
                                        Log.d(TAG,"testEventNoSessionNoData.onEvent");
                                        assertTrue(md == mMediaDrm);
                                        assertTrue(event == 2);
                                        assertTrue(extra == 456);
                                        assertTrue(sessionId == null);
                                        assertTrue(data == null);
                                        mGotEvent = true;
                                        mLock.notify();
                                    }
                                }
                            });
                    }
                } catch (MediaDrmException e) {
                    e.printStackTrace();
                    fail();
                }

                Looper.loop();  // Blocks forever until Looper.quit() is called.
            }
        }.start();

        // wait for mMediaDrm to be created
        synchronized(mLock) {
            try {
                mLock.wait(1000);
            } catch (Exception e) {
            }
        }
        assertTrue(mMediaDrm != null);

        mGotEvent = false;
        mMediaDrm.setPropertyString("mock-send-event", "2 456");

        synchronized(mLock) {
            try {
                mLock.wait(1000);
            } catch (Exception e) {
            }
        }

        mLooper.quit();
        assertTrue(mGotEvent);
    }

    public void testEventWithSessionAndData() throws Exception {
        if (!isMockPluginInstalled()) {
            return;
        }


        new Thread() {
            @Override
            public void run() {
                // Set up a looper to be used by mMediaPlayer.
                Looper.prepare();

                // Save the looper so that we can terminate this thread
                // after we are done with it.
                mLooper = Looper.myLooper();

                try {
                    mMediaDrm = new MediaDrm(mockScheme);

                    final byte[] expected_sessionId = mMediaDrm.openSession();
                    final byte[] expected_data = {0x10, 0x11, 0x12, 0x13, 0x14,
                                                  0x15, 0x16, 0x17, 0x18, 0x19};

                    mMediaDrm.setPropertyByteArray("mock-event-session-id", expected_sessionId);
                    mMediaDrm.setPropertyByteArray("mock-event-data", expected_data);

                    synchronized(mLock) {
                        mLock.notify();

                        mMediaDrm.setOnEventListener(new MediaDrm.OnEventListener() {
                                @Override
                                public void onEvent(MediaDrm md, byte[] sessionId, int event,
                                                    int extra, byte[] data) {
                                    synchronized(mLock) {
                                        Log.d(TAG,"testEventWithSessoinAndData.onEvent");
                                        assertTrue(md == mMediaDrm);
                                        assertTrue(event == 1);
                                        assertTrue(extra == 123);
                                        assertTrue(Arrays.equals(sessionId, expected_sessionId));
                                        assertTrue(Arrays.equals(data, expected_data));
                                        mGotEvent = true;
                                        mLock.notify();
                                    }
                                }
                            });
                    }
                } catch (MediaDrmException e) {
                    e.printStackTrace();
                    fail();
                }

                Looper.loop();  // Blocks forever until Looper.quit() is called.
            }
        }.start();

        // wait for mMediaDrm to be created
        synchronized(mLock) {
            try {
                mLock.wait(1000);
            } catch (Exception e) {
            }
        }
        assertTrue(mMediaDrm != null);

        mGotEvent = false;
        mMediaDrm.setPropertyString("mock-send-event", "1 123");

        synchronized(mLock) {
            try {
                mLock.wait(1000);
            } catch (Exception e) {
            }
        }

        mLooper.quit();
        assertTrue(mGotEvent);
    }
}
