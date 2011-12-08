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
import android.graphics.Bitmap;
import android.graphics.Picture;
import android.os.Looper;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebView.PictureListener;
import android.webkit.WebViewClient;

import junit.framework.Assert;

/**
 * This class is used to determine when a page has finished loading.
 *
 * initializeWebView sets a WebViewClient, WebChromeClient, and a
 * PictureListener. If a tests provides its own handler, it must call
 * the corresponding WaitForLoadUrl.on* function.
 *
 * This class only really works correctly with a single WebView active because
 * it acts as a singleton.
 */
class WaitForLoadUrl extends WebViewClient {
    private static final WaitForLoadUrl sInstance = new WaitForLoadUrl();

    /**
     * The maximum time, in milliseconds (10 seconds) to wait for a load
     * to be triggered.
     */
    private static final long LOAD_TIMEOUT = 10000;

    /**
     * Set to true after onPageFinished is called.
     */
    private boolean mLoaded;

    /**
     * Set to true after onNewPicture is called. Reset when onPageFinished
     * is called.
     */
    private boolean mNewPicture;

    /**
     * The progress, in percentage, of the page load. Valid values are between
     * 0 and 100.
     */
    private int mProgress;

    /**
     * Private constructor enforces singleton behavior.
     */
    private WaitForLoadUrl() {
    }

    /**
     * Returns the singleton instance.
     */
    public static WaitForLoadUrl getInstance() {
        return sInstance;
    }

    /**
     * Called from WaitForNewPicture, this is used to indicate that
     * the page has been drawn.
     */
    synchronized public void onNewPicture() {
        mNewPicture = true;
        this.notifyAll();
    }

    /**
     * Called from WaitForLoadedClient, this is used to clear the picture
     * draw state so that draws before the URL begins loading don't count.
     */
    synchronized public void onPageStarted() {
        mNewPicture = false; // Earlier paints won't count.
    }

    /**
     * Called from WaitForLoadedClient, this is used to indicate that
     * the page is loaded, but not drawn yet.
     */
    synchronized public void onPageFinished() {
        mLoaded = true;
        this.notifyAll();
    }

    /**
     * Called from the WebChrome client, this sets the current progress
     * for a page.
     * @param progress The progress made so far between 0 and 100.
     */
    synchronized public void onProgressChanged(int progress) {
        mProgress = progress;
        this.notifyAll();
    }

    /**
     * Sets the WebViewClient, WebChromeClient, and PictureListener for a
     * WebView to prepare it for the waitForLoadComplete call. If one
     * of these handlers needs to be changed, the onPageFinished,
     * onProgressChanged, or onNewPicture must be called from the callback
     * class.
     */
    public void initializeWebView(InstrumentationTestCase test,
            final WebView view) {
        Runnable setup = new Runnable() {
            @Override
            public void run() {
                view.setWebViewClient(new WaitForLoadedClient());
                view.setPictureListener(new WaitForNewPicture());
                view.setWebChromeClient(new WaitForProgressClient());
            }
        };
        if (isUiThread()) {
            setup.run();
        } else {
            try {
                test.runTestOnUiThread(setup);
            } catch (Throwable t) {
                Assert.fail("Error initializing WebView for waitForLoadUrl");
            }
        }
        clearLoad();
    }

    /**
     * Called whenever a load has been completed so that a subsequent call to
     * waitForLoadComplete doesn't return immediately. This must be called only
     * after onPageFinished is received for a loadUrl call or else a callback
     * will change the state before a subsequent load begins and
     * waitForLoadComplete will not work properly. Normally this call is not
     * necessary as it is automatically called as part of waitFor.
     */
    synchronized public void clearLoad() {
        mLoaded = false;
        mNewPicture = false;
        mProgress = 0;
    }

    /**
     * Wait for a page onPageFinished, onNewPicture and
     * onProgressChange to reach 100. If that does not occur
     * before LOAD_TIMEOUT expires there will be a test failure.
     *
     * This call may be made on the UI thread or a test thread.
     * @see WaitForLoadUrl#initializeWebView
     */
    public void waitForLoadComplete(WebView webView) {
        waitFor(webView, new WaitCheck() {
            @Override
            public boolean isDone() {
                return mLoaded && mNewPicture && mProgress == 100;
            }
        });
    }

    /**
     * Waits for the waitCheck condition to be true or the test times out.
     * The load state is cleared after waiting.
     * @param webView The WebView for which the test is running.
     * @param waitCheck Contains the condition to be checked.
     */
    private void waitFor(WebView webView, WaitCheck waitCheck) {
        if (isUiThread()) {
            waitOnUiThread(webView, waitCheck);
        } else {
            waitOnTestThread(waitCheck);
        }
        clearLoad();
    }

    /**
     * Uses a polling mechanism, while pumping messages to check when the
     * waitCheck condition is true.
     * @param webView The WebView for which the test is running.
     * @param waitCheck Contains the condition to be checked.
     */
    private void waitOnUiThread(final WebView webView,
            final WaitCheck waitCheck) {
        new PollingCheck(LOAD_TIMEOUT) {
            @Override
            protected boolean check() {
                pumpMessages(webView);
                synchronized(this) {
                    return waitCheck.isDone();
                }
            }
        }.run();
    }

    /**
     * Uses a wait/notify to check when the waitCheck condition is true.
     * @param webView The WebView for which the test is running.
     * @param waitCheck Contains the condition to be checked.
     */
    private synchronized void waitOnTestThread(WaitCheck waitCheck) {
        try {
            long waitEnd = SystemClock.uptimeMillis() + LOAD_TIMEOUT;
            long timeRemaining = LOAD_TIMEOUT;
            while (!waitCheck.isDone() && timeRemaining > 0) {
                this.wait(timeRemaining);
                timeRemaining = waitEnd - SystemClock.uptimeMillis();
            }
        } catch (InterruptedException e) {
            // We'll just drop out of the loop and fail
        }
        Assert.assertTrue("Load failed to complete before timeout",
                waitCheck.isDone());
    }

    /**
     * Pumps all currently-queued messages in the UI thread and then exits.
     * This is useful to force processing while running tests in the UI thread.
     */
    private static void pumpMessages(WebView webView) {
        class ExitLoopException extends RuntimeException {
        }

        // Force loop to exit when processing this. Loop.quit() doesn't
        // work because this is the main Loop.
        webView.getHandler().post(new Runnable() {
            @Override
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

    /**
     * Returns true if the current thread is the UI thread based on the
     * Looper.
     */
    private static boolean isUiThread() {
        return (Looper.myLooper() == Looper.getMainLooper());
    }

    private interface WaitCheck {
        public boolean isDone();
    }

    /**
     * A WebChromeClient used to capture the onProgressChanged for use
     * in waitFor functions. If a test must override the WebChromeClient,
     * it can derive from this class or call WaitForLoadUrl.onProgressChanged
     * directly.
     */
    public static class WaitForProgressClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            WaitForLoadUrl.getInstance().onProgressChanged(newProgress);
        }
    }

    /**
     * A WebViewClient that captures the onPageFinished for use in
     * waitFor functions. Using initializeWebView sets the WaitForLoadedClient
     * into the WebView. If a test needs to set a specific WebViewClient and
     * needs the waitForLoadComplete capability then it should derive from
     * WaitForLoadedClient or call WaitForLoadUrl.onPageFinished.
     */
    public static class WaitForLoadedClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            WaitForLoadUrl.getInstance().onPageFinished();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            WaitForLoadUrl.getInstance().onPageStarted();
        }
    }

    /**
     * A PictureListener that captures the onNewPicture for use in
     * waitForLoadComplete. Using initializeWebView sets the PictureListener
     * into the WebView. If a test needs to set a specific PictureListener and
     * needs the waitForLoadComplete capability then it should call
     * WaitForLoadUrl.onNewPicture.
     */
    private static class WaitForNewPicture implements PictureListener {
        @Override
        public void onNewPicture(WebView view, Picture picture) {
            WaitForLoadUrl.getInstance().onNewPicture();
        }
    }
}