/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.cts.tradefed.util;

import android.cts.util.ReportLog;

/**
 * ReportLog for host tests
 * Note that setTestInfo should be set before throwing report
 */
public class HostReportLog extends ReportLog {

    private String mKey;

    /**
     * @param deviceSerial serial number of the device
     */
    public HostReportLog(String deviceSerial) {
        final StackTraceElement e = Thread.currentThread().getStackTrace()[1];
        mKey = String.format("%s#%s#%s", deviceSerial, e.getClassName(), e.getMethodName());
    }

    public void submit() {
        ResultStore.addResult(mKey, this);
    }
}
