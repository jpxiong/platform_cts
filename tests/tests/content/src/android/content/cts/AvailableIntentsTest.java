/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.content.cts;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Settings;
import android.test.AndroidTestCase;

import java.util.List;

@TestTargetClass(Intent.class)
public class AvailableIntentsTest extends AndroidTestCase {
    private static final String NORMAL_URL = "http://www.google.com/";
    private static final String SECURE_URL = "https://www.google.com/";

    /**
     * Assert target intent can be handled by at least one Activity.
     * @param intent - the Intent will be handled.
     */
    private void assertCanBeHandled(final Intent intent) {
        PackageManager packageManager = mContext.getPackageManager();
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(intent, 0);
        assertNotNull(resolveInfoList);
        // one or more activity can handle this intent.
        assertTrue(resolveInfoList.size() > 0);
    }

    /**
     * Test ACTION_VIEW when url is http://web_address,
     * it will open a browser window to the URL specified.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "Intent",
        args = {java.lang.String.class, android.net.Uri.class}
    )
    public void testViewNormalUrl() {
        Uri uri = Uri.parse(NORMAL_URL);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        assertCanBeHandled(intent);
    }

    /**
     * Test ACTION_VIEW when url is https://web_address,
     * it will open a browser window to the URL specified.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "Intent",
        args = {java.lang.String.class, android.net.Uri.class}
    )
    public void testViewSecureUrl() {
        Uri uri = Uri.parse(SECURE_URL);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        assertCanBeHandled(intent);
    }

    /**
     * Test ACTION_WEB_SEARCH when url is http://web_address,
     * it will open a browser window to the URL specified.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "Intent",
        args = {java.lang.String.class, android.net.Uri.class}
    )
    public void testWebSearchNormalUrl() {
        Uri uri = Uri.parse(NORMAL_URL);
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, uri);
        assertCanBeHandled(intent);
    }

    /**
     * Test ACTION_WEB_SEARCH when url is https://web_address,
     * it will open a browser window to the URL specified.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "Intent",
        args = {java.lang.String.class, android.net.Uri.class}
    )
    public void testWebSearchSecureUrl() {
        Uri uri = Uri.parse(SECURE_URL);
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, uri);
        assertCanBeHandled(intent);
    }

    /**
     * Test ACTION_WEB_SEARCH when url is empty string,
     * google search will be applied for the plain text.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "Intent",
        args = {java.lang.String.class, android.net.Uri.class}
    )
    public void testWebSearchPlainText() {
        String searchString = "where am I?";
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, searchString);
        assertCanBeHandled(intent);
    }

    /**
     * Test ACTION_CALL when uri is a phone number, it will call the entered phone number.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "Intent",
        args = {java.lang.String.class, android.net.Uri.class}
    )
    public void testCallPhoneNumber() {
        Uri uri = Uri.parse("tel:2125551212");
        Intent intent = new Intent(Intent.ACTION_CALL, uri);
        assertCanBeHandled(intent);
    }

    /**
     * Test ACTION_DIAL when uri is a phone number, it will dial the entered phone number.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "Intent",
        args = {java.lang.String.class, android.net.Uri.class}
    )
    public void testDialPhoneNumber() {
        PackageManager packageManager = mContext.getPackageManager();
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            Uri uri = Uri.parse("tel:(212)5551212");
            Intent intent = new Intent(Intent.ACTION_DIAL, uri);
            assertCanBeHandled(intent);
        }
    }

    /**
     * Test ACTION_DIAL when uri is a phone number, it will dial the entered phone number.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "Intent",
        args = {java.lang.String.class, android.net.Uri.class}
    )
    public void testDialVoicemail() {
        PackageManager packageManager = mContext.getPackageManager();
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            Uri uri = Uri.parse("voicemail:");
            Intent intent = new Intent(Intent.ACTION_DIAL, uri);
            assertCanBeHandled(intent);
        }
    }

    /**
     * Test start camera by intent
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "Intent",
        args = {java.lang.String.class}
    )
    public void testCamera() {
        PackageManager packageManager = mContext.getPackageManager();
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            assertCanBeHandled(intent);

            intent.setAction(MediaStore.ACTION_VIDEO_CAPTURE);
            assertCanBeHandled(intent);

            intent.setAction(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            assertCanBeHandled(intent);

            intent.setAction(MediaStore.INTENT_ACTION_VIDEO_CAMERA);
            assertCanBeHandled(intent);
        }
    }

    public void testSettings() {
        assertCanBeHandled(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_ADD_ACCOUNT));
        assertCanBeHandled(new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_APN_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:com.android.cts.stub")));

        // TODO: Uncomment in HC. This appears to be broken in Froyo and Gingerbread...
        // assertCanBeHandled(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));

        assertCanBeHandled(new Intent(Settings.ACTION_APPLICATION_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_DATE_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_LOCALE_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_MEMORY_CARD_SETTINGS));

        // TODO: Seems to not work at on NS, Xoom, Xoom WiFi
        // assertCanBeHandled(new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS));

        assertCanBeHandled(new Intent(Settings.ACTION_PRIVACY_SETTINGS));

        // TODO: Seems to not work at on NS, Xoom, Xoom WiFi
        // assertCanBeHandled(new Intent(Settings.ACTION_QUICK_LAUNCH_SETTINGS));

        assertCanBeHandled(new Intent(Settings.ACTION_SEARCH_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_SECURITY_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_SOUND_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_SYNC_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_SYSTEM_UPDATE_SETTINGS));
        // TODO : Seems to not work if there is no user dictonary support on the device
        // assertCanBeHandled(new Intent(Settings.ACTION_USER_DICTIONARY_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_WIFI_IP_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_WIFI_SETTINGS));
        assertCanBeHandled(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
    }

    /**
     * Test add event in calendar
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "Intent",
        args = {java.lang.String.class}
    )
    public void testCalendarAddAppointment() {
        Intent addAppointmentIntent = new Intent(Intent.ACTION_EDIT);
        addAppointmentIntent.setType("vnd.android.cursor.item/event");
        assertCanBeHandled(addAppointmentIntent);
    }

    /**
     * Test view call logs
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "Intent",
        args = {java.lang.String.class}
    )
    public void testContactsCallLogs() {
        PackageManager packageManager = mContext.getPackageManager();
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("vnd.android.cursor.dir/calls");
            assertCanBeHandled(intent);
        }
    }

    /**
     * Test view music playback
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "Intent",
        args = {java.lang.String.class}
    )
    public void testMusicPlayback() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(ContentUris.withAppendedId(
                MediaStore.Audio.Media.INTERNAL_CONTENT_URI, 1), "audio/*");
        assertCanBeHandled(intent);
    }

    /**
     * Test launch inbox view of Mms application
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "Intent",
        args = {java.lang.String.class}
    )
    public void testViewMessageInbox() {
        PackageManager packageManager = mContext.getPackageManager();
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setType("vnd.android.cursor.dir/mms");
            assertCanBeHandled(intent);

            intent.setType("vnd.android-dir/mms-sms");
            assertCanBeHandled(intent);
        }
    }
}
