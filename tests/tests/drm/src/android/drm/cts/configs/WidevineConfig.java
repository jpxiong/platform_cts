/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.drm.cts.configs;

import java.util.HashMap;

import android.drm.cts.Config;

public class WidevineConfig implements Config {
    private static WidevineConfig sInstance = new WidevineConfig();
    private WidevineConfig() {}
    public static WidevineConfig getInstance() {
        return sInstance;
    }
    public String getPluginName() {
        return "Widevine DRM";
    }
    public String getMimeType() {
        return "video/wvm";
    }
    public String getAccountId() {
        return "01234567";
    }
    public String getRightsPath() {
        return "/sdcard/dummy_passthru_rights.xml";
    }
    public String getContentPath() {
        return Settings.DRM_ASSET_URI;
    }
    public HashMap<String, String> getInfoOfRegistration() {
        return sInfoOfRegistration;
    }
    public HashMap<String, String> getInfoOfUnregistration() {
        return sInfoOfUnregistration;
    }
    public HashMap<String, String> getInfoOfRightsAcquisition() {
        return sInfoOfRightsAcquisition;
    }

    private static class Settings {
        public static String WIDEVINE_MIME_TYPE = "video/wvm";
        public static String DRM_ASSET_URI = "http://seawwws001.cdn.shibboleth.tv/videos/qa/trailers_d_ch_444169.wvm";
        public static String DRM_SERVER_URI = "http://wstfcps005.shibboleth.tv/widevine/cypherpc/cgi-bin/GetEMMs.cgi";
        public static String DEVICE_ID = "device12345"; // use a unique device ID
        public static String PORTAL_NAME = "YouTube";

        // test with a sizeable block of user data...
        public static String USER_DATA =
                "01234567890123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789";
    };

    private static HashMap<String, String> sInfoOfRegistration = new HashMap<String, String>();
    private static HashMap<String, String> sInfoOfUnregistration = new HashMap<String, String>();
    private static HashMap<String, String> sInfoOfRightsAcquisition = new HashMap<String, String>();

    static {
        sInfoOfRegistration.put("WVPortalKey", Settings.PORTAL_NAME);

        sInfoOfUnregistration.put("WVPortalKey", Settings.PORTAL_NAME);

        sInfoOfRightsAcquisition.put("WVDRMServerKey", Settings.DRM_SERVER_URI);
        sInfoOfRightsAcquisition.put("WVAssetURIKey", Settings.DRM_ASSET_URI);
        sInfoOfRightsAcquisition.put("WVDeviceIDKey", Settings.DEVICE_ID);
        sInfoOfRightsAcquisition.put("WVPortalKey", Settings.PORTAL_NAME);
        sInfoOfRightsAcquisition.put("WVCAUserDataKey", Settings.USER_DATA);
    }
}
