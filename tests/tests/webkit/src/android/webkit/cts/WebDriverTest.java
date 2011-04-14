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

package android.webkit.cts;

import android.test.ActivityInstrumentationTestCase2;
import android.webkit.webdriver.WebDriver;

import static android.webkit.cts.TestHtmlConstants.HELLO_WORLD_URL;

/**
 * Tests for {@link android.webkit.webdriver.WebDriver}.
 */
public class WebDriverTest extends
        ActivityInstrumentationTestCase2<WebDriverStubActivity>{
    private WebDriver mDriver;
    private CtsTestServer mWebServer;

    public WebDriverTest() {
        super(WebDriverStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDriver = getActivity().getDriver();
        mWebServer = new CtsTestServer(getActivity(), false);
    }

    @Override
    protected void tearDown() throws Exception {
        mWebServer.shutdown();
        super.tearDown();
    }

    public void testGetIsBlocking() {
        mDriver.get(mWebServer.getDelayedAssetUrl(HELLO_WORLD_URL));
        assertTrue(mDriver.getPageSource().contains("hello world!"));
    }
}
