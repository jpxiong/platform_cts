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

public class MarlinConfig implements Config {
    private static MarlinConfig sInstance = new MarlinConfig();
    private MarlinConfig() {}
    public static MarlinConfig getInstance() {
        return sInstance;
    }
    public String getPluginName() {
        return "Marlin";
    }
    public String getMimeType() {
        return "application/vnd.marlin.drm.actiontoken+xml";
    }
    public String getAccountId() {
        return "01234567";
    }
    public String getRightsPath() {
        return "/sdcard/dummy_marlin_rights.xml";
    }
    public String getContentPath() {
        return "/sdcard/dummy_marlin_content.MNV";
    }
    public HashMap<String, String> getInfoOfRegistration() {
        return sInfoOfRegistration;
    }
    public HashMap<String, String> getInfoOfUnregistration() {
        return sInfoOfUnregistration;
    }
    public HashMap<String, String> getInfoOfRightsAcquisition(){
        return sInfoOfRightsAcquisition;
    }

    private static HashMap<String, String> sInfoOfRegistration = new HashMap<String, String>();
    private static HashMap<String, String> sInfoOfUnregistration = new HashMap<String, String>();
    private static HashMap<String, String> sInfoOfRightsAcquisition = new HashMap<String, String>();

    static {
        sInfoOfRegistration.put("Content-Type", "application/x-www-form-urlencoded");
        sInfoOfRegistration.put("User-Agent", "PSN/ActionTokenAcquisition");
        sInfoOfRegistration.put("X-Mln-ATAcq-Version", "1.1");
        sInfoOfRegistration.put("X-UC-LoginId", "user1");
        sInfoOfRegistration.put("X-UC-EPassword", "password");
        sInfoOfRegistration.put("X-Mln-ATAcq-Platform", "pc");
        sInfoOfRegistration.put("MarlinServerAddress", "43.17.164.1:8080");
        sInfoOfRegistration.put("AT-Type", "user-bound");

        sInfoOfUnregistration.put("Content-Type", "application/x-www-form-urlencoded");
        sInfoOfUnregistration.put("User-Agent", "PSN/ActionTokenAcquisition");
        sInfoOfUnregistration.put("X-Mln-ATAcq-Version", "1.1");
        sInfoOfUnregistration.put("X-UC-LoginId", "user1");
        sInfoOfUnregistration.put("X-UC-EPassword", "password");
        sInfoOfUnregistration.put("X-Mln-ATAcq-Platform", "pc");
        sInfoOfUnregistration.put("MarlinServerAddress", "43.17.164.1:8080");
        sInfoOfUnregistration.put("AT-Type", "user-bound");

        sInfoOfRightsAcquisition.put("Content-Type", "application/x-www-form-urlencoded");
        sInfoOfRightsAcquisition.put("User-Agent", "PSN/ActionTokenAcquisition");
        sInfoOfRightsAcquisition.put("X-Mln-ATAcq-Version", "1.1");
        sInfoOfRightsAcquisition.put("X-UC-LoginId", "user1");
        sInfoOfRightsAcquisition.put("X-UC-EPassword", "password");
        sInfoOfRightsAcquisition.put("X-Mln-ATAcq-Platform", "pc");
        sInfoOfRightsAcquisition.put("contentid", "00000001");
        sInfoOfRightsAcquisition.put("MarlinServerAddress", "43.17.164.1:8080");
        sInfoOfRightsAcquisition.put("AT-Type", "user-bound");
    }
}
