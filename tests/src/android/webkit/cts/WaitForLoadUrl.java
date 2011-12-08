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

import android.cts.util.PollingCheck;
import android.graphics.Picture;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;
import android.webkit.WebView.PictureListener;
import android.webkit.WebViewClient;

/**
 * This class is used to determine when a page has finished loading. A
 * WebViewClient listens for the "onPageFinished" and then waits for the
 * corresponding onNewPicture as received from a PictureListener.
 *
 * initializeWebView sets a WebViewClient and a PictureListener. If you
 * provide your own PictureListener, you must call WaitForLoadUrl.onNewPicture.
 * If you need to provide your own WebViewClient, then extend
 * WaitForLoadedClient or call WaitForLoadUrl.onPageFinished.
 *
 * This class only really works correctly with a single WebView active because
 * the picture listening is a static.
 */
class WaitForLoadUrl extends WebViewClient {
    /**
     * A WebViewClient that captures the onPageFinished for use in
     * waitForLoadComplete. Using initializeWebView sets the WaitForLoadedClient
     * into the WebView. If a test needs to set a specific WebViewClient and
     * needs the waitForLoadComplete capability then it should derive from
     * WaitForLoadedClient or call WaitForLoadUrl.onPageFinished.
     */
    public static class WaitForLoadedClient extends WebViewClient {
        public void onPageFinished(WebView view, String url) {
            WaitForLoadUrl.onPageFinished();
        }
    }

    /**
     * A Picture that captures the onNewPicture for use in
     * waitForLoadComplete. Using initializeWebView sets the PictureListener
     * into the WebView. If a test needs to set a specific PictureListener and
     * needs the waitForLoadComplete capability then it should call
     * WaitForLoadUrl.onNewPicture.
     */
    private static class WaitForNewPicture implements PictureListener {
        public void onNewPicture(WebView view, Picture picture) {
            WaitForLoadUrl.onNewPicture();
        }
    }

    /**
     * Set to true after onPageFinished is called.
     */
    private static boolean mLoaded;

    /**
     * Set to true after onNewPicture is called. Reset when onPageFinished
     * is called.
     */
    private static boolean mNewPicture;

    /**
     * Called from WaitForNewPicture, this is used to indicate that
     * the page has been drawn.
     */
    public static void onNewPicture() {
        mNewPicture = true;
    }

    /**
     * Called from WaitForLoadedClient, this is used to indicate that
     * the page is loaded, but not drawn yet.
     */
    public static void onPageFinished() {
        mLoaded = true;
        mNewPicture = false; // Earlier paints won't count.
    }

    /**
     * Sets the WebViewClient and PictureListener for a WebView to prepare
     * it for the waitForLoadComplete call.
     */
    public static void initializeWebView(WebView view) {
        view.setWebViewClient(new WaitForLoadedClient());
        view.setPictureListener(new WaitForNewPicture());
        clearLoad();
    }

    /**
     * Called whenever a load has been completed so that a subsequent call to
     * waitForLoadComplete doesn't return immediately.
     */
    public static void clearLoad() {
        mLoaded = false;
        mNewPicture = false;
    }

    /**
     * Wait for a page to load for at least timeout milliseconds.
     * This function requires that initializeWebView be called on the webView
     * prior to running or a different means for triggering onPageFinished
     * and onNewPicture calls. A failure for the page to be loaded by timeout
     * milliseconds will result in an test failure.
     *
     * This call may be made on the UI thread or a test thread.
     */
    public static void waitForLoadComplete(long timeout) {
        final boolean isUIThread = (Looper.myLooper() == Looper.getMainLooper());
        // on the UI thread
        new PollingCheck(timeout) {
            @Override
            protected boolean check() {
                if (isUIThread) {
                    pumpMessages();
                }
                boolean isLoaded = mLoaded && mNewPicture;
                if (isLoaded) {
                    clearLoad();
                }
                return isLoaded;
            }
        }.run();
    }

    /**
     * Pumps all currently-queued messages in the UI thread and then exits.
     * This is useful to force processing while running tests in the UI thread.
     */
    public static void pumpMessages() {
        class ExitLoopException extends RuntimeException {
        }

        // Force loop to exit when processing this. Loop.quit() doesn't
        // work because this is the main Loop.
        Handler handler = new Handler();
        handler.post(new Runnable() {
            public void run() {
                throw new ExitLoopException(); // exit loop!
            }
        });
        try {
            // Pump messages until our message gets through.
            Looper.loop();
        } catch (ExitLoopException e) {
        }
    }

}
