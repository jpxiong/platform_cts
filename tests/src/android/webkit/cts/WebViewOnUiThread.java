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

import android.graphics.Bitmap;
import android.os.Looper;
import android.test.InstrumentationTestCase;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import junit.framework.Assert;


/**
 * Many tests need to run WebView code in the UI thread. This class
 * wraps a WebView so that calls are ensured to arrive on the UI Thread.
 */
public class WebViewOnUiThread {
    private InstrumentationTestCase mTest;
    private WebView mWebView;

    /**
     * Initializes the webView with a WebViewClient, WebChromeClient,
     * and PictureListener as per WaitForLoadUrl.initializeWebView
     * to prepare for loadUrl.
     *
     * This method should be called during setUp so as to reinitialize
     * between calls.
     *
     * @param test The test in which this is being run.
     * @param webView The webView that the methods should call.
     * @see WaitForLoadUrl#initializeWebView
     * @see loadUrlAndWaitForCompletion
     */
    public WebViewOnUiThread(InstrumentationTestCase test, WebView webView) {
        mTest = test;
        mWebView = webView;
        WaitForLoadUrl.getInstance().initializeWebView(mTest, mWebView);
    }

    public void setWebViewClient(final WebViewClient webViewClient) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.setWebViewClient(webViewClient);
            }
        });
    }

    public void setWebChromeClient(final WebChromeClient webChromeClient) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.setWebChromeClient(webChromeClient);
            }
        });
    }

    public void clearCache(final boolean includeDiskFiles) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.clearCache(includeDiskFiles);
            }
        });
    }

    public void clearHistory() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.clearHistory();
            }
        });
    }

    public void requestFocus() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.requestFocus();
            }
        });
    }

    public void zoomIn() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.zoomIn();
            }
        });
    }

    public void reload() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.reload();
            }
        });
    }

    public void loadUrl(final String url) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl(url);
            }
        });
    }

    /**
     * Calls loadUrl on the WebView and then waits onPageFinished,
     * onNewPicture and onProgressChange to reach 100.
     * Test fails if the load timeout elapses.
     * @param url The URL to load.
     */
    public void loadUrlAndWaitForCompletion(final String url) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl(url);
            }
        });
        WaitForLoadUrl.getInstance().waitForLoadComplete(mWebView);
    }

    public String getTitle() {
        class TitleGetter implements Runnable {
            private String mTitle;

            @Override
            public void run() {
                mTitle = mWebView.getTitle();
            }

            public String getTitle() {
                return mTitle;
            }
        }
        TitleGetter titleGetter = new TitleGetter();
        runOnUiThread(titleGetter);
        return titleGetter.getTitle();
    }

    public WebSettings getSettings() {
        class SettingsGetter implements Runnable {
            private WebSettings mWebSettings;

            @Override
            public void run() {
                mWebSettings = mWebView.getSettings();
            }

            public WebSettings getSettings() {
                return mWebSettings;
            }
        }

        SettingsGetter settingsGetter = new SettingsGetter();
        runOnUiThread(settingsGetter);
        return settingsGetter.getSettings();
    }

    public WebBackForwardList copyBackForwardList() {
        class BackForwardCopier implements Runnable {
            private WebBackForwardList mBackForwardList;

            @Override
            public void run() {
                mBackForwardList = mWebView.copyBackForwardList();
            }

            public WebBackForwardList getBackForwardList() {
                return mBackForwardList;
            }
        }
        BackForwardCopier backForwardCopier = new BackForwardCopier();
        runOnUiThread(backForwardCopier);
        return backForwardCopier.getBackForwardList();
    }

    /**
     * @see android.webkit.WebView.WebView#
     */
    public Bitmap getFavicon() {
        class FaviconGetter implements Runnable {
            private Bitmap mFavicon;

            @Override
            public void run() {
                mFavicon = mWebView.getFavicon();
            }

            public Bitmap getFavicon() {
                return mFavicon;
            }
        }
        FaviconGetter favIconGetter = new FaviconGetter();
        runOnUiThread(favIconGetter);
        return favIconGetter.getFavicon();
    }

    public String getUrl() {
        class UrlGetter implements Runnable {
            private String mUrl;

            @Override
            public void run() {
                mUrl = mWebView.getUrl();
            }

            public String getUrl() {
                return mUrl;
            }
        }
        UrlGetter urlGetter = new UrlGetter();
        runOnUiThread(urlGetter);
        return urlGetter.getUrl();
    }

    public int getProgress() {
        class ProgressGetter implements Runnable {
            private int mProgress;

            @Override
            public void run() {
                mProgress = mWebView.getProgress();
            }

            public int getProgress() {
                return mProgress;
            }
        }
        ProgressGetter progressGetter = new ProgressGetter();
        runOnUiThread(progressGetter);
        return progressGetter.getProgress();
    }

    /**
     * Helper for running code on the UI thread where an exception is
     * a test failure. If this is already the UI thread then it runs
     * the code immediately.
     *
     * @see runTestOnUiThread
     * @param r The code to run in the UI thread
     */
    public void runOnUiThread(Runnable r) {
        try {
            if (isUiThread()) {
                r.run();
            } else {
                mTest.runTestOnUiThread(r);
            }
        } catch (Throwable t) {
            Assert.fail("Unexpected error while running on UI thread.");
        }
    }

    /**
     * Accessor for underlying WebView.
     * @return The WebView being wrapped by this class.
     */
    public WebView getWebView() {
        return mWebView;
    }

    /**
     * Returns true if the current thread is the UI thread based on the
     * Looper.
     */
    private static boolean isUiThread() {
        return (Looper.myLooper() == Looper.getMainLooper());
    }
}
