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

package com.android.cts;

public interface DeviceObserver {

    public static final int FAIL = -1;
    public static final int SUCCESS = 1;

    /**
     * Update Test running status when running in batch mode.
     *
     * @param test The Test to update.
     * @param status The status to be updated.
     */
    void notifyTestStatus(final Test test, final String status);

    /**
     * Update result code and failed message.
     *
     * @param resCode test result code value.
     * @param failedMessage failed message content.
     * @param stackTrace stack trace information.
     */
    void notifyUpdateResult(final int resCode, final String failedMessage,
            final String stackTrace);

    /**
     * Notify after installing apk complete on the {@link TestDevice}
     * no matter succeeded or failed.
     *
     * @param resultCode the result code of installation.
     */
    void notifyInstallingComplete(final int resultCode);

    /**
     * Notify after uninstalling apk complete on the {@link TestDevice}.
     *
     * @param resultCode the result code of uninstallation.
     */
    void notifyUninstallingComplete(final int resultCode);

    /**
     * Notify after installing apk timeout on the {@link TestDevice}
     *
     * @param testDevice the {@link TestDevice} whose install action timeout.
     */
    void notifyInstallingTimeout(final TestDevice testDevice);

    /**
     * Notify after uninstalling apk timeout.
     *
     * @param testDevice the {@link TestDevice} whose install action timeout
     */
    void notifyUninstallingTimeout(final TestDevice testDevice);

    /**
     * Notify after a {@link TestDevice}, which is used in testing,
     * is disconnected to the {@link TestHost}.
     *
     */
    void notifyTestingDeviceDisconnected();
}
