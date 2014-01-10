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

package android.print.cts;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.CancellationSignal.OnCancelListener;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintAttributes.Margins;
import android.print.PrintAttributes.MediaSize;
import android.print.PrintAttributes.Resolution;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentAdapter.LayoutResultCallback;
import android.print.PrintDocumentAdapter.WriteResultCallback;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.PrinterCapabilitiesInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.print.cts.services.FirstPrintService;
import android.print.cts.services.SecondPrintService;
import android.print.cts.services.StubPrintService;
import android.printservice.PrintJob;
import android.printservice.PrinterDiscoverySession;
import android.util.DisplayMetrics;

import android.support.test.uiautomator.UiAutomatorTestCase;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

/**
 * This test verifies that the system respects the {@link PrintDocumentAdapter}
 * contract and invokes all callbacks as expected.
 */
public class PrintDocumentAdapterContractTest extends UiAutomatorTestCase {

    private static final long OPERATION_TIMEOUT = 10000;

    private static final String ARG_PRIVILEGED_OPS = "ARG_PRIVILEGED_OPS";

    private static final String PRINT_SPOOLER_PACKAGE_NAME = "com.android.printspooler";

    private PrintDocumentAdapterContractActivity mActivity;

    private Locale mOldLocale;

    @Override
    public void setUp() throws Exception {
        // Make sure we start with a clean slate.
        clearPrintSpoolerData();

        // Workaround for dexmaker bug: https://code.google.com/p/dexmaker/issues/detail?id=2
        // Dexmaker is used by mockito.
        System.setProperty("dexmaker.dexcache", getInstrumentation()
                .getTargetContext().getCacheDir().getPath());

        // Set to US locale.
        Resources resources = getInstrumentation().getTargetContext().getResources();
        Configuration oldConfiguration = resources.getConfiguration();
        if (!oldConfiguration.locale.equals(Locale.US)) {
            mOldLocale = oldConfiguration.locale;
            DisplayMetrics displayMetrics = resources.getDisplayMetrics();
            Configuration newConfiguration = new Configuration(oldConfiguration);
            newConfiguration.locale = Locale.US;
            resources.updateConfiguration(newConfiguration, displayMetrics);
        }

        // Create the activity for the right locale.
        createActivity();
    }

    @Override
    public void tearDown() throws Exception {
        // Done with the activity.
        getActivity().finish();

        // Restore the locale if needed.
        if (mOldLocale != null) {
            Resources resources = getInstrumentation().getTargetContext().getResources();
            DisplayMetrics displayMetrics = resources.getDisplayMetrics();
            Configuration newConfiguration = new Configuration(resources.getConfiguration());
            newConfiguration.locale = mOldLocale;
            mOldLocale = null;
            resources.updateConfiguration(newConfiguration, displayMetrics);
        }

        // Make sure the spooler is cleaned.
        clearPrintSpoolerData();
    }

    public void testNoPrintOptionsOrPrinterChange() throws Exception {
        // Configure the print services.
        FirstPrintService.setImpl(new SimpleTwoPrintersService());
        SecondPrintService.setImpl(null);

        final CallCounter layoutCallCounter = new CallCounter();
        final CallCounter writeCallCounter = new CallCounter();
        final CallCounter finishCallCounter = new CallCounter();

        // Create a mock print adapter.
        final PrintDocumentAdapter adapter = createMockPrintDocumentAdapter(
            new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                LayoutResultCallback callback = (LayoutResultCallback) invocation.getArguments()[3];
                PrintDocumentInfo info = new PrintDocumentInfo.Builder("Test")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1)
                        .build();
                callback.onLayoutFinished(info, false);
                layoutCallCounter.call();
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                PageRange[] pages = (PageRange[]) args[0];
                ParcelFileDescriptor fd = (ParcelFileDescriptor) args[1];
                WriteResultCallback callback = (WriteResultCallback) args[3];
                fd.close();
                callback.onWriteFinished(pages);
                // Mark write was called.
                writeCallCounter.call();
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Mark finish was called.
                finishCallCounter.call();
                return null;
            }
        });

        // Start printing.
        print(adapter);

        // Wait for write.
        waitForWriteForAdapterCallback(writeCallCounter);

        // Select the second printer.
        selectPrinter("Second printer");

        // Wait for layout.
        waitForLayoutAdapterCallbackCount(layoutCallCounter, 2);

        // Click the print button.
        clickPrintButton();

        // Wait for finish.
        waitForAdapterCallbackFinish(finishCallCounter);

        // Verify the expected calls.
        InOrder inOrder = inOrder(adapter);

        // Start is always called first.
        inOrder.verify(adapter).onStart();

        // Start is always followed by a layout. The PDF printer is selected if
        // there are other printers but none of them was used.
        PrintAttributes firstOldAttributes = new PrintAttributes.Builder().build();
        PrintAttributes firstNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.NA_LETTER)
                .setResolution(new Resolution("PDF resolution", "PDF resolution", 300, 300))
                .setMinMargins(Margins.NO_MARGINS)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, firstOldAttributes, firstNewAttributes, true);

        // We always ask for the first page for preview.
        PageRange[] firstPages = new PageRange[] {new PageRange(0, 0)};
        inOrder.verify(adapter).onWrite(eq(firstPages), any(ParcelFileDescriptor.class),
                any(CancellationSignal.class), any(WriteResultCallback.class));

        // We selected the second printer which does not support the media
        // size that was selected, so a new layout happens as the size changed.
        // Since we passed false to the layout callback meaning that the content
        // didn't change, there shouldn't be a next call to write.
        PrintAttributes secondOldAttributes = firstNewAttributes;
        PrintAttributes secondNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.ISO_A3)
                .setResolution(new Resolution("300x300", "300x300", 300, 300))
                .setMinMargins(Margins.NO_MARGINS)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, secondOldAttributes, secondNewAttributes, true);

        // When print is pressed we ask for a layout which is *not* for preview.
        verifyLayoutCall(inOrder, adapter, secondNewAttributes, secondNewAttributes, false);

        // When print is pressed we ask for all selected pages.
        PageRange[] secondPages = new PageRange[] {PageRange.ALL_PAGES};
        inOrder.verify(adapter).onWrite(eq(secondPages), any(ParcelFileDescriptor.class),
                any(CancellationSignal.class), any(WriteResultCallback.class));

        // Finish is always called last.
        inOrder.verify(adapter).onFinish();

        // No other call are expected.
        verifyNoMoreInteractions(adapter);
    }

    public void testNoPrintOptionsOrPrinterChangeCanceled() throws Exception {
        // Configure the print services.
        FirstPrintService.setImpl(new SimpleTwoPrintersService());
        SecondPrintService.setImpl(null);

        final CallCounter writeCallCounter = new CallCounter();
        final CallCounter finishCallCounter = new CallCounter();

        // Create a mock print adapter.
        final PrintDocumentAdapter adapter = createMockPrintDocumentAdapter(
            new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                LayoutResultCallback callback = (LayoutResultCallback)
                        invocation.getArguments()[3];
                PrintDocumentInfo info = new PrintDocumentInfo.Builder("Test")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build();
                callback.onLayoutFinished(info, false);
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                PageRange[] pages = (PageRange[]) args[0];
                ParcelFileDescriptor fd = (ParcelFileDescriptor) args[1];
                WriteResultCallback callback = (WriteResultCallback) args[3];
                fd.close();
                callback.onWriteFinished(pages);
                // Mark write was called.
                writeCallCounter.call();
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Mark finish was called.
                finishCallCounter.call();
                return null;
            }
        });

        // Start printing.
        print(adapter);

        // Wait for write.
        waitForWriteForAdapterCallback(writeCallCounter);

        // Cancel the printing.
        UiDevice.getInstance().pressBack();

        // Wait for finish.
        waitForAdapterCallbackFinish(finishCallCounter);

        // Verify the expected calls.
        InOrder inOrder = inOrder(adapter);

        // Start is always called first.
        inOrder.verify(adapter).onStart();

        // Start is always followed by a layout. The PDF printer is selected if
        // there are other printers but none of them was used.
        PrintAttributes firstOldAttributes = new PrintAttributes.Builder().build();
        PrintAttributes firstNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.NA_LETTER)
                .setResolution(new Resolution("PDF resolution", "PDF resolution", 300, 300))
                .setMinMargins(Margins.NO_MARGINS)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, firstOldAttributes, firstNewAttributes, true);

        // We always ask for the first page for preview.
        PageRange[] firstPages = new PageRange[] {new PageRange(0, 0)};
        inOrder.verify(adapter).onWrite(eq(firstPages), any(ParcelFileDescriptor.class),
                any(CancellationSignal.class), any(WriteResultCallback.class));

        // Finish is always called last.
        inOrder.verify(adapter).onFinish();

        // No other call are expected.
        verifyNoMoreInteractions(adapter);
    }

    public void testPrintOptionsChangeAndNoPrinterChange() throws Exception {
        // Configure the print services.
        FirstPrintService.setImpl(new SimpleTwoPrintersService());
        SecondPrintService.setImpl(null);

        final CallCounter layoutCallCounter = new CallCounter();
        final CallCounter writeCallCounter = new CallCounter();
        final CallCounter finishCallCounter = new CallCounter();

        // Create a mock print adapter.
        final PrintDocumentAdapter adapter = createMockPrintDocumentAdapter(
            new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                LayoutResultCallback callback = (LayoutResultCallback)
                        invocation.getArguments()[3];
                PrintDocumentInfo info = new PrintDocumentInfo.Builder("Test")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build();
                callback.onLayoutFinished(info, false);
                // Mark layout was called.
                layoutCallCounter.call();
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                PageRange[] pages = (PageRange[]) args[0];
                ParcelFileDescriptor fd = (ParcelFileDescriptor) args[1];
                WriteResultCallback callback = (WriteResultCallback) args[3];
                fd.close();
                callback.onWriteFinished(pages);
                // Mark write was called.
                writeCallCounter.call();
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Mark finish was called.
                finishCallCounter.call();
                return null;
            }
        });

        // Start printing.
        print(adapter);

        // Wait for write.
        waitForWriteForAdapterCallback(writeCallCounter);

        // Select the second printer.
        selectPrinter("Second printer");

        // Wait for layout.
        waitForLayoutAdapterCallbackCount(layoutCallCounter, 2);

        // Change the orientation.
        changeOrientation("Landscape");

        // Wait for layout.
        waitForLayoutAdapterCallbackCount(layoutCallCounter, 3);

        // Change the media size.
        changeMediaSize("ISO A4");

        // Wait for layout.
        waitForLayoutAdapterCallbackCount(layoutCallCounter, 4);

        // Change the color.
        changeColor("Black & White");

        // Wait for layout.
        waitForLayoutAdapterCallbackCount(layoutCallCounter, 5);

        // Click the print button.
        clickPrintButton();

        // Wait for a finish.
        waitForAdapterCallbackFinish(finishCallCounter);

        // Verify the expected calls.
        InOrder inOrder = inOrder(adapter);

        // Start is always called first.
        inOrder.verify(adapter).onStart();

        // Start is always followed by a layout. The PDF printer is selected if
        // there are other printers but none of them was used.
        PrintAttributes firstOldAttributes = new PrintAttributes.Builder().build();
        PrintAttributes firstNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.NA_LETTER)
                .setResolution(new Resolution("PDF resolution", "PDF resolution", 300, 300))
                .setMinMargins(Margins.NO_MARGINS)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, firstOldAttributes, firstNewAttributes, true);

        // We always ask for the first page for preview.
        PageRange[] firstPages = new PageRange[] {new PageRange(0, 0)};
        inOrder.verify(adapter).onWrite(eq(firstPages), any(ParcelFileDescriptor.class),
                any(CancellationSignal.class), any(WriteResultCallback.class));

        // We selected the second printer which does not support the media
        // size that was selected, so a new layout happens as the size changed.
        // Since we passed false to the layout callback meaning that the content
        // didn't change, there shouldn't be a next call to write.
        PrintAttributes secondOldAttributes = firstNewAttributes;
        PrintAttributes secondNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.ISO_A3)
                .setResolution(new Resolution("300x300", "300x300", 300, 300))
                .setMinMargins(Margins.NO_MARGINS)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, secondOldAttributes, secondNewAttributes, true);

        // We changed the orientation which triggers a layout. Since we passed
        // false to the layout callback meaning that the content didn't change,
        // there shouldn't be a next call to write.
        PrintAttributes thirdOldAttributes = secondNewAttributes;
        PrintAttributes thirdNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.ISO_A3.asLandscape())
                .setResolution(new Resolution("300x300", "300x300", 300, 300))
                .setMinMargins(Margins.NO_MARGINS)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, thirdOldAttributes, thirdNewAttributes, true);

        // We changed the media size which triggers a layout. Since we passed
        // false to the layout callback meaning that the content didn't change,
        // there shouldn't be a next call to write.
        PrintAttributes fourthOldAttributes = thirdNewAttributes;
        PrintAttributes fourthNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.ISO_A4.asLandscape())
                .setResolution(new Resolution("300x300", "300x300", 300, 300))
                .setMinMargins(Margins.NO_MARGINS)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, fourthOldAttributes, fourthNewAttributes, true);

        // We changed the color which triggers a layout. Since we passed
        // false to the layout callback meaning that the content didn't change,
        // there shouldn't be a next call to write.
        PrintAttributes fifthOldAttributes = fourthNewAttributes;
        PrintAttributes fifthNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.ISO_A4.asLandscape())
                .setResolution(new Resolution("300x300", "300x300", 300, 300))
                .setMinMargins(Margins.NO_MARGINS)
                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                .build();
        verifyLayoutCall(inOrder, adapter, fifthOldAttributes, fifthNewAttributes, true);

        // When print is pressed we ask for a layout which is *not* for preview.
        verifyLayoutCall(inOrder, adapter, fifthNewAttributes, fifthNewAttributes, false);

        // When print is pressed we ask for all selected pages.
        PageRange[] secondPages = new PageRange[] {PageRange.ALL_PAGES};
        inOrder.verify(adapter).onWrite(eq(secondPages), any(ParcelFileDescriptor.class),
                any(CancellationSignal.class), any(WriteResultCallback.class));

        // Finish is always called last.
        inOrder.verify(adapter).onFinish();

        // No other call are expected.
        verifyNoMoreInteractions(adapter);
    }

    public void testPrintOptionsChangeAndPrinterChange() throws Exception {
        // Configure the print services.
        FirstPrintService.setImpl(new SimpleTwoPrintersService());
        SecondPrintService.setImpl(null);

        final CallCounter layoutCallCounter = new CallCounter();
        final CallCounter writeCallCounter = new CallCounter();
        final CallCounter finishCallCounter = new CallCounter();

        // Create a mock print adapter.
        final PrintDocumentAdapter adapter = createMockPrintDocumentAdapter(
            new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                LayoutResultCallback callback = (LayoutResultCallback)
                        invocation.getArguments()[3];
                PrintDocumentInfo info = new PrintDocumentInfo.Builder("Test")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build();
                callback.onLayoutFinished(info, false);
                // Mark layout was called.
                layoutCallCounter.call();
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                PageRange[] pages = (PageRange[]) args[0];
                ParcelFileDescriptor fd = (ParcelFileDescriptor) args[1];
                WriteResultCallback callback = (WriteResultCallback) args[3];
                fd.close();
                callback.onWriteFinished(pages);
                // Mark write was called.
                writeCallCounter.call();
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Mark finish was called.
                finishCallCounter.call();
                return null;
            }
        });

        // Start printing.
        print(adapter);

        // Wait for write.
        waitForWriteForAdapterCallback(writeCallCounter);

        // Select the second printer.
        selectPrinter("Second printer");

        // Wait for layout.
        waitForLayoutAdapterCallbackCount(layoutCallCounter, 2);

        // Change the color.
        changeColor("Black & White");

        // Wait for layout.
        waitForLayoutAdapterCallbackCount(layoutCallCounter, 3);

        // Change the printer to one which supports the current media size.
        // Select the second printer.
        selectPrinter("First printer");

        // Wait for layout.
        waitForLayoutAdapterCallbackCount(layoutCallCounter, 4);

        // Click the print button.
        clickPrintButton();

        // Wait for a finish.
        waitForAdapterCallbackFinish(finishCallCounter);

        // Verify the expected calls.
        InOrder inOrder = inOrder(adapter);

        // Start is always called first.
        inOrder.verify(adapter).onStart();

        // Start is always followed by a layout. The PDF printer is selected if
        // there are other printers but none of them was used.
        PrintAttributes firstOldAttributes = new PrintAttributes.Builder().build();
        PrintAttributes firstNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.NA_LETTER)
                .setResolution(new Resolution("PDF resolution", "PDF resolution", 300, 300))
                .setMinMargins(Margins.NO_MARGINS).setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, firstOldAttributes, firstNewAttributes, true);

        // We always ask for the first page for preview.
        PageRange[] firstPages = new PageRange[] {new PageRange(0, 0)};
        inOrder.verify(adapter).onWrite(eq(firstPages), any(ParcelFileDescriptor.class),
                any(CancellationSignal.class), any(WriteResultCallback.class));

        // We changed the printer and the new printer does not support the
        // selected media size in which case the default media size of the
        // printer is used resulting in a layout pass. Same for margins.
        PrintAttributes secondOldAttributes = firstNewAttributes;
        PrintAttributes secondNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.ISO_A3)
                .setResolution(new Resolution("PDF resolution", "PDF resolution", 300, 300))
                .setMinMargins(new Margins(0, 0, 0, 0))
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, secondOldAttributes, secondNewAttributes, true);

        // We changed the printer and the new printer does not support the
        // current color in which case the default color for the selected
        // printer is used resulting in a layout pass.
        PrintAttributes thirdOldAttributes = secondNewAttributes;
        PrintAttributes thirdNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.ISO_A3)
                .setResolution(new Resolution("PDF resolution", "PDF resolution", 300, 300))
                .setMinMargins(new Margins(0, 0, 0, 0))
                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                .build();
        verifyLayoutCall(inOrder, adapter, thirdOldAttributes, thirdNewAttributes, true);

        // We changed the printer to one that does not support the current
        // media size in which case we pick the default media size for the
        // new printer which results in a layout pass. Same for color.
        PrintAttributes fourthOldAttributes = thirdNewAttributes;
        PrintAttributes fourthNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.ISO_A4)
                .setResolution(new Resolution("PDF resolution", "PDF resolution", 300, 300))
                .setMinMargins(new Margins(200, 200, 200, 200))
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, fourthOldAttributes, fourthNewAttributes, true);

        // When print is pressed we ask for a layout which is *not* for preview.
        verifyLayoutCall(inOrder, adapter, fourthNewAttributes, fourthNewAttributes, false);

        // When print is pressed we ask for all selected pages.
        PageRange[] secondPages = new PageRange[] {PageRange.ALL_PAGES};
        inOrder.verify(adapter).onWrite(eq(secondPages), any(ParcelFileDescriptor.class),
                any(CancellationSignal.class), any(WriteResultCallback.class));

        // Finish is always called last.
        inOrder.verify(adapter).onFinish();

        // No other call are expected.
        verifyNoMoreInteractions(adapter);
    }

    public void testPrintOptionsChangeAndNoPrinterChangeAndContentChange()
            throws Exception {
        // Configure the print services.
        FirstPrintService.setImpl(new SimpleTwoPrintersService());
        SecondPrintService.setImpl(null);

        final CallCounter layoutCallCounter = new CallCounter();
        final CallCounter writeCallCounter = new CallCounter();
        final CallCounter finishCallCounter = new CallCounter();

        // Create a mock print adapter.
        final PrintDocumentAdapter adapter = createMockPrintDocumentAdapter(
            new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                LayoutResultCallback callback = (LayoutResultCallback) invocation.getArguments()[3];
                PrintDocumentInfo info = new PrintDocumentInfo.Builder("Test")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1)
                        .build();
                // The content changes after every layout.
                callback.onLayoutFinished(info, true);
                // Mark layout was called.
                layoutCallCounter.call();
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                PageRange[] pages = (PageRange[]) args[0];
                ParcelFileDescriptor fd = (ParcelFileDescriptor) args[1];
                WriteResultCallback callback = (WriteResultCallback) args[3];
                fd.close();
                callback.onWriteFinished(pages);
                // Mark write was called.
                writeCallCounter.call();
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Mark finish was called.
                finishCallCounter.call();
                return null;
            }
        });

        // Start printing.
        print(adapter);

        // Wait for write.
        waitForWriteForAdapterCallback(writeCallCounter);

        // Select the second printer.
        selectPrinter("Second printer");

        // Wait for layout.
        waitForLayoutAdapterCallbackCount(layoutCallCounter, 2);

        // Click the print button.
        clickPrintButton();

        // Wait for a finish.
        waitForAdapterCallbackFinish(finishCallCounter);

        // Verify the expected calls.
        InOrder inOrder = inOrder(adapter);

        // Start is always called first.
        inOrder.verify(adapter).onStart();

        // Start is always followed by a layout. The PDF printer is selected if
        // there are other printers but none of them was used.
        PrintAttributes firstOldAttributes = new PrintAttributes.Builder().build();
        PrintAttributes firstNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.NA_LETTER)
                .setResolution(new Resolution("PDF resolution", "PDF resolution", 300, 300))
                .setMinMargins(Margins.NO_MARGINS).setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, firstOldAttributes, firstNewAttributes, true);

        // We always ask for the first page for preview.
        PageRange[] firstPages = new PageRange[] {new PageRange(0, 0)};
        inOrder.verify(adapter).onWrite(eq(firstPages), any(ParcelFileDescriptor.class),
                any(CancellationSignal.class), any(WriteResultCallback.class));

        // We selected the second printer which does not support the media
        // size that was selected, so a new layout happens as the size changed.
        PrintAttributes secondOldAttributes = firstNewAttributes;
        PrintAttributes secondNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.ISO_A3)
                .setResolution(new Resolution("300x300", "300x300", 300, 300))
                .setMinMargins(Margins.NO_MARGINS).setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, secondOldAttributes, secondNewAttributes, true);

        // In the layout callback we reported that the content changed,
        // so the previously written page has to be written again.
        PageRange[] secondPages = new PageRange[] {new PageRange(0, 0)};
        inOrder.verify(adapter).onWrite(eq(secondPages), any(ParcelFileDescriptor.class),
                any(CancellationSignal.class), any(WriteResultCallback.class));

        // When print is pressed we ask for a layout which is *not* for preview.
        verifyLayoutCall(inOrder, adapter, secondNewAttributes, secondNewAttributes, false);

        // When print is pressed we ask for all selected pages.
        PageRange[] thirdPages = new PageRange[] {PageRange.ALL_PAGES};
        inOrder.verify(adapter).onWrite(eq(thirdPages), any(ParcelFileDescriptor.class),
                any(CancellationSignal.class), any(WriteResultCallback.class));

        // Finish is always called last.
        inOrder.verify(adapter).onFinish();

        // No other call are expected.
        verifyNoMoreInteractions(adapter);
    }

    public void testNewPrinterSupportsSelectedPrintOptions() throws Exception {
        // Configure the print services.
        FirstPrintService.setImpl(new SimpleTwoPrintersService());
        SecondPrintService.setImpl(null);

        final CallCounter writeCallCounter = new CallCounter();
        final CallCounter finishCallCounter = new CallCounter();

        // Create a mock print adapter.
        final PrintDocumentAdapter adapter = createMockPrintDocumentAdapter(
            new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                LayoutResultCallback callback = (LayoutResultCallback) invocation.getArguments()[3];
                PrintDocumentInfo info = new PrintDocumentInfo.Builder("Test")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1)
                        .build();
                // The content changes after every layout.
                callback.onLayoutFinished(info, false);
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                PageRange[] pages = (PageRange[]) args[0];
                ParcelFileDescriptor fd = (ParcelFileDescriptor) args[1];
                WriteResultCallback callback = (WriteResultCallback) args[3];
                fd.close();
                callback.onWriteFinished(pages);
                // Mark write was called.
                writeCallCounter.call();
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Mark finish was called.
                finishCallCounter.call();
                return null;
            }
        });

        // Start printing.
        print(adapter);

        // Wait for write.
        waitForWriteForAdapterCallback(writeCallCounter);

        // Select the third printer.
        selectPrinter("Third printer");

        // Click the print button.
        clickPrintButton();

        // Wait for a finish.
        waitForAdapterCallbackFinish(finishCallCounter);

        // Verify the expected calls.
        InOrder inOrder = inOrder(adapter);

        // Start is always called first.
        inOrder.verify(adapter).onStart();

        // Start is always followed by a layout. The PDF printer is selected if
        // there are other printers but none of them was used.
        PrintAttributes firstOldAttributes = new PrintAttributes.Builder().build();
        PrintAttributes firstNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.NA_LETTER)
                .setResolution(new Resolution("PDF resolution", "PDF resolution", 300, 300))
                .setMinMargins(Margins.NO_MARGINS).setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, firstOldAttributes, firstNewAttributes, true);

        // We always ask for the first page for preview.
        PageRange[] firstPages = new PageRange[] {new PageRange(0, 0)};
        inOrder.verify(adapter).onWrite(eq(firstPages), any(ParcelFileDescriptor.class),
                any(CancellationSignal.class), any(WriteResultCallback.class));

        // When print is pressed we ask for a layout which is *not* for preview.
        verifyLayoutCall(inOrder, adapter, firstNewAttributes, firstNewAttributes, false);

        // When print is pressed we ask for all selected pages.
        PageRange[] thirdPages = new PageRange[] {PageRange.ALL_PAGES};
        inOrder.verify(adapter).onWrite(eq(thirdPages), any(ParcelFileDescriptor.class),
                any(CancellationSignal.class), any(WriteResultCallback.class));

        // Finish is always called last.
        inOrder.verify(adapter).onFinish();

        // No other call are expected.
        verifyNoMoreInteractions(adapter);
    }

    public void testNothingChangesAllPagesWrittenFirstTime() throws Exception {
        // Configure the print services.
        FirstPrintService.setImpl(new SimpleTwoPrintersService());
        SecondPrintService.setImpl(null);

        final CallCounter layoutCallCounter = new CallCounter();
        final CallCounter writeCallCounter = new CallCounter();
        final CallCounter finishCallCounter = new CallCounter();

        // Create a mock print adapter.
        final PrintDocumentAdapter adapter = createMockPrintDocumentAdapter(
            new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                LayoutResultCallback callback = (LayoutResultCallback) invocation.getArguments()[3];
                PrintDocumentInfo info = new PrintDocumentInfo.Builder("Test")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(3)
                        .build();
                callback.onLayoutFinished(info, false);
                // Mark layout was called.
                layoutCallCounter.call();
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ParcelFileDescriptor fd = (ParcelFileDescriptor) args[1];
                WriteResultCallback callback = (WriteResultCallback) args[3];
                fd.close();
                callback.onWriteFinished(new PageRange[] {PageRange.ALL_PAGES});
                // Mark write was called.
                writeCallCounter.call();
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Mark finish was called.
                finishCallCounter.call();
                return null;
            }
        });

        // Start printing.
        print(adapter);

        // Wait for write.
        waitForWriteForAdapterCallback(writeCallCounter);

        // Select the second printer.
        selectPrinter("Second printer");

        // Wait for layout.
        waitForLayoutAdapterCallbackCount(layoutCallCounter, 2);

        // Click the print button.
        clickPrintButton();

        // Wait for a finish.
        waitForAdapterCallbackFinish(finishCallCounter);

        // Verify the expected calls.
        InOrder inOrder = inOrder(adapter);

        // Start is always called first.
        inOrder.verify(adapter).onStart();

        // Start is always followed by a layout. The PDF printer is selected if
        // there are other printers but none of them was used.
        PrintAttributes firstOldAttributes = new PrintAttributes.Builder().build();
        PrintAttributes firstNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.NA_LETTER)
                .setResolution(new Resolution("PDF resolution", "PDF resolution", 300, 300))
                .setMinMargins(Margins.NO_MARGINS).setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, firstOldAttributes, firstNewAttributes, true);

        // We always ask for the first page for preview.
        PageRange[] firstPages = new PageRange[] {new PageRange(0, 0)};
        inOrder.verify(adapter).onWrite(eq(firstPages), any(ParcelFileDescriptor.class),
                any(CancellationSignal.class), any(WriteResultCallback.class));

        // We selected the second printer which does not support the media
        // size that was selected, so a new layout happens as the size changed.
        PrintAttributes secondOldAttributes = firstNewAttributes;
        PrintAttributes secondNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.ISO_A3)
                .setResolution(new Resolution("300x300", "300x300", 300, 300))
                .setMinMargins(Margins.NO_MARGINS).setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, secondOldAttributes, secondNewAttributes, true);

        // In the layout callback we reported that the content didn't change,
        // and we wrote all pages in the write call while being asked only
        // for the first page. Hence, all pages were written and they didn't
        // change, therefore no subsequent write call should happen.

        // When print is pressed we ask for a layout which is *not* for preview.
        verifyLayoutCall(inOrder, adapter, secondNewAttributes, secondNewAttributes, false);

        // In the layout callback we reported that the content didn't change,
        // and we wrote all pages in the write call while being asked only
        // for the first page. Hence, all pages were written and they didn't
        // change, therefore no subsequent write call should happen.

        // Finish is always called last.
        inOrder.verify(adapter).onFinish();

        // No other call are expected.
        verifyNoMoreInteractions(adapter);
    }

    public void testCancelLongRunningLayout() throws Exception {
        // Configure the print services.
        FirstPrintService.setImpl(new SimpleTwoPrintersService());
        SecondPrintService.setImpl(null);

        final CallCounter layoutCallCounter = new CallCounter();
        final CallCounter finishCallCounter = new CallCounter();

        // Create a mock print adapter.
        final PrintDocumentAdapter adapter = createMockPrintDocumentAdapter(
            new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                CancellationSignal cancellation = (CancellationSignal) invocation.getArguments()[2];
                final LayoutResultCallback callback = (LayoutResultCallback) invocation
                        .getArguments()[3];
                cancellation.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel() {
                        callback.onLayoutCancelled();
                    }
                });
                layoutCallCounter.call();
                return null;
            }
        }, null, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Mark finish was called.
                finishCallCounter.call();
                return null;
            }
        });

        // Start printing.
        print(adapter);

        // Wait for layout.
        waitForLayoutAdapterCallbackCount(layoutCallCounter, 1);

        // Cancel printing.
        UiDevice.getInstance().pressBack(); // wakes up the device.
        UiDevice.getInstance().pressBack();

        // Wait for a finish.
        waitForAdapterCallbackFinish(finishCallCounter);

        // Verify the expected calls.
        InOrder inOrder = inOrder(adapter);

        // Start is always called first.
        inOrder.verify(adapter).onStart();

        // Start is always followed by a layout. The PDF printer is selected if
        // there are other printers but none of them was used.
        PrintAttributes firstOldAttributes = new PrintAttributes.Builder().build();
        PrintAttributes firstNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.NA_LETTER)
                .setResolution(new Resolution("PDF resolution", "PDF resolution", 300, 300))
                .setMinMargins(Margins.NO_MARGINS).setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, firstOldAttributes, firstNewAttributes, true);

        // Finish is always called last.
        inOrder.verify(adapter).onFinish();

        // No other call are expected.
        verifyNoMoreInteractions(adapter);
    }

    public void testCancelLongRunningWrite() throws Exception {
        // Configure the print services.
        FirstPrintService.setImpl(new SimpleTwoPrintersService());
        SecondPrintService.setImpl(null);

        final CallCounter writeCallCounter = new CallCounter();
        final CallCounter finishCallCounter = new CallCounter();

        // Create a mock print adapter.
        final PrintDocumentAdapter adapter = createMockPrintDocumentAdapter(
            new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                LayoutResultCallback callback = (LayoutResultCallback) invocation.getArguments()[3];
                PrintDocumentInfo info = new PrintDocumentInfo.Builder("Test")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1)
                        .build();
                callback.onLayoutFinished(info, false);
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                final ParcelFileDescriptor fd = (ParcelFileDescriptor) args[1];
                final CancellationSignal cancellation = (CancellationSignal) args[2];
                final WriteResultCallback callback = (WriteResultCallback) args[3];
                cancellation.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel() {
                        try {
                            fd.close();
                        } catch (IOException ioe) {
                            /* ignore */
                        }
                        callback.onWriteCancelled();
                    }
                });
                // Mark write was called.
                writeCallCounter.call();
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Mark finish was called.
                finishCallCounter.call();
                return null;
            }
        });

        // Start printing.
        print(adapter);

        // Wait for write.
        waitForWriteForAdapterCallback(writeCallCounter);

        // Cancel printing.
        UiDevice.getInstance().pressBack(); // wakes up the device.
        UiDevice.getInstance().pressBack();

        // Wait for a finish.
        waitForAdapterCallbackFinish(finishCallCounter);

        // Verify the expected calls.
        InOrder inOrder = inOrder(adapter);

        // Start is always called first.
        inOrder.verify(adapter).onStart();

        // Start is always followed by a layout. The PDF printer is selected if
        // there are other printers but none of them was used.
        PrintAttributes firstOldAttributes = new PrintAttributes.Builder().build();
        PrintAttributes firstNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.NA_LETTER)
                .setResolution(new Resolution("PDF resolution", "PDF resolution", 300, 300))
                .setMinMargins(Margins.NO_MARGINS).setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, firstOldAttributes, firstNewAttributes, true);

        // We always ask for the first page for preview.
        PageRange[] firstPages = new PageRange[] {new PageRange(0, 0)};
        inOrder.verify(adapter).onWrite(eq(firstPages), any(ParcelFileDescriptor.class),
                any(CancellationSignal.class), any(WriteResultCallback.class));

        // Finish is always called last.
        inOrder.verify(adapter).onFinish();

        // No other call are expected.
        verifyNoMoreInteractions(adapter);
    }

    public void testFailedLayout() throws Exception {
        // Configure the print services.
        FirstPrintService.setImpl(new SimpleTwoPrintersService());
        SecondPrintService.setImpl(null);

        final CallCounter layoutCallCounter = new CallCounter();
        final CallCounter finishCallCounter = new CallCounter();

        // Create a mock print adapter.
        final PrintDocumentAdapter adapter = createMockPrintDocumentAdapter(
            new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                LayoutResultCallback callback = (LayoutResultCallback) invocation.getArguments()[3];
                callback.onLayoutFailed(null);
                // Mark layout was called.
                layoutCallCounter.call();
                return null;
            }
        }, null, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Mark finish was called.
                finishCallCounter.call();
                return null;
            }
        });

        // Start printing.
        print(adapter);

        // Wait for layout.
        waitForLayoutAdapterCallbackCount(layoutCallCounter, 1);

        // Cancel printing.
        UiDevice.getInstance().pressBack(); // wakes up the device.
        UiDevice.getInstance().pressBack();

        // Wait for a finish.
        waitForAdapterCallbackFinish(finishCallCounter);

        // Verify the expected calls.
        InOrder inOrder = inOrder(adapter);

        // Start is always called first.
        inOrder.verify(adapter).onStart();

        // Start is always followed by a layout. The PDF printer is selected if
        // there are other printers but none of them was used.
        PrintAttributes firstOldAttributes = new PrintAttributes.Builder().build();
        PrintAttributes firstNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.NA_LETTER)
                .setResolution(new Resolution("PDF resolution", "PDF resolution", 300, 300))
                .setMinMargins(Margins.NO_MARGINS).setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, firstOldAttributes, firstNewAttributes, true);

        // No write as layout failed.

        // Finish is always called last.
        inOrder.verify(adapter).onFinish();

        // No other call are expected.
        verifyNoMoreInteractions(adapter);
    }

    public void testFailedWrite() throws Exception {
        // Configure the print services.
        FirstPrintService.setImpl(new SimpleTwoPrintersService());
        SecondPrintService.setImpl(null);

        final CallCounter writeCallCounter = new CallCounter();
        final CallCounter finishCallCounter = new CallCounter();

        // Create a mock print adapter.
        final PrintDocumentAdapter adapter = createMockPrintDocumentAdapter(
            new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                LayoutResultCallback callback = (LayoutResultCallback) invocation.getArguments()[3];
                PrintDocumentInfo info = new PrintDocumentInfo.Builder("Test")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1)
                        .build();
                callback.onLayoutFinished(info, false);
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ParcelFileDescriptor fd = (ParcelFileDescriptor) args[1];
                WriteResultCallback callback = (WriteResultCallback) args[3];
                fd.close();
                callback.onWriteFailed(null);
                // Mark write was called.
                writeCallCounter.call();
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Mark finish was called.
                finishCallCounter.call();
                return null;
            }
        });

        // Start printing.
        print(adapter);

        // Wait for write.
        waitForWriteForAdapterCallback(writeCallCounter);

        // Cancel printing.
        UiDevice.getInstance().pressBack(); // wakes up the device.
        UiDevice.getInstance().pressBack();

        // Wait for a finish.
        waitForAdapterCallbackFinish(finishCallCounter);

        // Verify the expected calls.
        InOrder inOrder = inOrder(adapter);

        // Start is always called first.
        inOrder.verify(adapter).onStart();

        // Start is always followed by a layout. The PDF printer is selected if
        // there are other printers but none of them was used.
        PrintAttributes firstOldAttributes = new PrintAttributes.Builder().build();
        PrintAttributes firstNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.NA_LETTER)
                .setResolution(new Resolution("PDF resolution", "PDF resolution", 300, 300))
                .setMinMargins(Margins.NO_MARGINS).setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, firstOldAttributes, firstNewAttributes, true);

        // We always ask for the first page for preview.
        PageRange[] firstPages = new PageRange[] {new PageRange(0, 0)};
        inOrder.verify(adapter).onWrite(eq(firstPages), any(ParcelFileDescriptor.class),
                any(CancellationSignal.class), any(WriteResultCallback.class));

        // Finish is always called last.
        inOrder.verify(adapter).onFinish();

        // No other call are expected.
        verifyNoMoreInteractions(adapter);
    }

    public void testRequestedPagesNotWritten() throws Exception {
        // Configure the print services.
        FirstPrintService.setImpl(new SimpleTwoPrintersService());
        SecondPrintService.setImpl(null);

        final CallCounter writeCallCounter = new CallCounter();
        final CallCounter finishCallCounter = new CallCounter();

        // Create a mock print adapter.
        final PrintDocumentAdapter adapter = createMockPrintDocumentAdapter(
            new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                LayoutResultCallback callback = (LayoutResultCallback) invocation.getArguments()[3];
                PrintDocumentInfo info = new PrintDocumentInfo.Builder("Test")
                      .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1)
                      .build();
                callback.onLayoutFinished(info, false);
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ParcelFileDescriptor fd = (ParcelFileDescriptor) args[1];
                WriteResultCallback callback = (WriteResultCallback) args[3];
                fd.close();
                // Write wrong pages.
                callback.onWriteFinished(new PageRange[] {
                        new PageRange(Integer.MAX_VALUE,Integer.MAX_VALUE)});
                // Mark write was called.
                writeCallCounter.call();
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Mark finish was called.
                finishCallCounter.call();
                return null;
            }
        });

        // Start printing.
        print(adapter);

        // Wait for write.
        waitForWriteForAdapterCallback(writeCallCounter);

        // Cancel printing.
        UiDevice.getInstance().pressBack(); // wakes up the device.
        UiDevice.getInstance().pressBack();

        // Wait for a finish.
        waitForAdapterCallbackFinish(finishCallCounter);

        // Verify the expected calls.
        InOrder inOrder = inOrder(adapter);

        // Start is always called first.
        inOrder.verify(adapter).onStart();

        // Start is always followed by a layout. The PDF printer is selected if
        // there are other printers but none of them was used.
        PrintAttributes firstOldAttributes = new PrintAttributes.Builder().build();
        PrintAttributes firstNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.NA_LETTER)
                .setResolution(new Resolution("PDF resolution", "PDF resolution", 300, 300))
                .setMinMargins(Margins.NO_MARGINS).setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, firstOldAttributes, firstNewAttributes, true);

        // We always ask for the first page for preview.
        PageRange[] firstPages = new PageRange[] {new PageRange(0, 0)};
        inOrder.verify(adapter).onWrite(eq(firstPages), any(ParcelFileDescriptor.class),
                any(CancellationSignal.class), any(WriteResultCallback.class));

        // Finish is always called last.
        inOrder.verify(adapter).onFinish();

        // No other call are expected.
        verifyNoMoreInteractions(adapter);
    }

    public void testLayoutCallbackNotCalled() throws Exception {
        // Configure the print services.
        FirstPrintService.setImpl(new SimpleTwoPrintersService());
        SecondPrintService.setImpl(null);

        final CallCounter layoutCallCounter = new CallCounter();
        final CallCounter finishCallCounter = new CallCounter();

        // Create a mock print adapter.
        final PrintDocumentAdapter adapter = createMockPrintDocumentAdapter(
            new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Break the contract and never call the callback.
                // Mark layout called.
                layoutCallCounter.call();
                return null;
            }
        }, null, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Mark finish was called.
                finishCallCounter.call();
                return null;
            }
        });

        // Start printing.
        print(adapter);

        // Wait for layout.
        waitForLayoutAdapterCallbackCount(layoutCallCounter, 1);

        // Cancel printing.
        UiDevice.getInstance().pressBack(); // wakes up the device.
        UiDevice.getInstance().pressBack();

        // Wait for a finish.
        waitForAdapterCallbackFinish(finishCallCounter);

        // Verify the expected calls.
        InOrder inOrder = inOrder(adapter);

        // Start is always called first.
        inOrder.verify(adapter).onStart();

        // Start is always followed by a layout. The PDF printer is selected if
        // there are other printers but none of them was used.
        PrintAttributes firstOldAttributes = new PrintAttributes.Builder().build();
        PrintAttributes firstNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.NA_LETTER)
                .setResolution(new Resolution("PDF resolution", "PDF resolution", 300, 300))
                .setMinMargins(Margins.NO_MARGINS).setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, firstOldAttributes, firstNewAttributes, true);

        // Finish is always called last.
        inOrder.verify(adapter).onFinish();

        // No other call are expected.
        verifyNoMoreInteractions(adapter);
    }

    public void testEriteCallbackNotCalled() throws Exception {
        // Configure the print services.
        FirstPrintService.setImpl(new SimpleTwoPrintersService());
        SecondPrintService.setImpl(null);

        final CallCounter writeCallCounter = new CallCounter();
        final CallCounter finishCallCounter = new CallCounter();

        // Create a mock print adapter.
        final PrintDocumentAdapter adapter = createMockPrintDocumentAdapter(
            new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                LayoutResultCallback callback = (LayoutResultCallback) invocation.getArguments()[3];
                PrintDocumentInfo info = new PrintDocumentInfo.Builder("Test")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1)
                        .build();
                callback.onLayoutFinished(info, false);
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ParcelFileDescriptor fd = (ParcelFileDescriptor) args[1];
                fd.close();
                // Break the contract and never call the callback.
                // Mark write was called.
                writeCallCounter.call();
                return null;
            }
        }, new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Mark finish was called.
                finishCallCounter.call();
                return null;
            }
        });

        // Start printing.
        print(adapter);

        // Wait for write.
        waitForWriteForAdapterCallback(writeCallCounter);

        // Cancel printing.
        UiDevice.getInstance().pressBack(); // wakes up the device.
        UiDevice.getInstance().pressBack();

        // Wait for a finish.
        waitForAdapterCallbackFinish(finishCallCounter);

        // Verify the expected calls.
        InOrder inOrder = inOrder(adapter);

        // Start is always called first.
        inOrder.verify(adapter).onStart();

        // Start is always followed by a layout. The PDF printer is selected if
        // there are other printers but none of them was used.
        PrintAttributes firstOldAttributes = new PrintAttributes.Builder().build();
        PrintAttributes firstNewAttributes = new PrintAttributes.Builder()
                .setMediaSize(MediaSize.NA_LETTER)
                .setResolution(new Resolution("PDF resolution", "PDF resolution", 300, 300))
                .setMinMargins(Margins.NO_MARGINS).setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        verifyLayoutCall(inOrder, adapter, firstOldAttributes, firstNewAttributes, true);

        // We always ask for the first page for preview.
        PageRange[] firstPages = new PageRange[] {new PageRange(0, 0)};
        inOrder.verify(adapter).onWrite(eq(firstPages), any(ParcelFileDescriptor.class),
                any(CancellationSignal.class), any(WriteResultCallback.class));

        // Finish is always called last.
        inOrder.verify(adapter).onFinish();

        // No other call are expected.
        verifyNoMoreInteractions(adapter);
    }

    private void print(final PrintDocumentAdapter adapter) {
        // Initiate printing as if coming from the app.
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                PrintManager printManager = (PrintManager) getActivity()
                        .getSystemService(Context.PRINT_SERVICE);
                printManager.print("Print job", adapter, null);
            }
        });
    }

    private void waitForAdapterCallbackFinish(CallCounter counter) {
        waitForCallbackCallCount(counter, 1, "Did not get expected call to finish.");
    }

    private void waitForLayoutAdapterCallbackCount(CallCounter counter, int count) {
        waitForCallbackCallCount(counter, count, "Did not get expected call to layout.");
    }

    private void waitForWriteForAdapterCallback(CallCounter counter) {
        waitForCallbackCallCount(counter, 1, "Did not get expected call to write.");
    }

    private void waitForCallbackCallCount(CallCounter counter, int count, String message) {
        try {
            counter.waitForCount(count, OPERATION_TIMEOUT);
        } catch (TimeoutException te) {
            fail(message);
        }
    }

    private void selectPrinter(String printerName) throws UiObjectNotFoundException {
        UiObject destinationSpinner = new UiObject(new UiSelector().resourceId(
                "com.android.printspooler:id/destination_spinner"));
        destinationSpinner.click();
        UiObject printerOption = new UiObject(new UiSelector().text(printerName));
        printerOption.click();
    }

    private void changeOrientation(String orientation) throws UiObjectNotFoundException {
        UiObject orientationSpinner = new UiObject(new UiSelector().resourceId(
                "com.android.printspooler:id/orientation_spinner"));
        orientationSpinner.click();
        UiObject orientationOption = new UiObject(new UiSelector().text(orientation));
        orientationOption.click();
    }

    private void changeMediaSize(String mediaSize) throws UiObjectNotFoundException {
        UiObject mediaSizeSpinner = new UiObject(new UiSelector().resourceId(
                "com.android.printspooler:id/paper_size_spinner"));
        mediaSizeSpinner.click();
        UiObject mediaSizeOption = new UiObject(new UiSelector().text(mediaSize));
        mediaSizeOption.click();
    }

    private void changeColor(String color) throws UiObjectNotFoundException {
        UiObject colorSpinner = new UiObject(new UiSelector().resourceId(
                "com.android.printspooler:id/color_spinner"));
        colorSpinner.click();
        UiObject colorOption = new UiObject(new UiSelector().text(color));
        colorOption.click();
    }

    private void clickPrintButton() throws UiObjectNotFoundException {
        UiObject printButton = new UiObject(new UiSelector().resourceId(
                "com.android.printspooler:id/print_button"));
        printButton.click();
    }

    private PrintDocumentAdapterContractActivity getActivity() {
        return mActivity;
    }

    private void createActivity() {
        mActivity = launchActivity(
                getInstrumentation().getTargetContext().getPackageName(),
                PrintDocumentAdapterContractActivity.class, null);
    }

    private void clearPrintSpoolerData() throws Exception {
        IPrivilegedOperations privilegedOps = IPrivilegedOperations.Stub.asInterface(
                getParams().getBinder(ARG_PRIVILEGED_OPS));
        privilegedOps.clearApplicationUserData(PRINT_SPOOLER_PACKAGE_NAME);
    }

    private PrintDocumentAdapter createMockPrintDocumentAdapter(Answer<Void> layoutAnswer,
            Answer<Void> writeAnswer, Answer<Void> finishAnswer) {
        // Create a mock print adapter.
        PrintDocumentAdapter adapter = mock(PrintDocumentAdapter.class);
        if (layoutAnswer != null) {
            doAnswer(layoutAnswer).when(adapter).onLayout(any(PrintAttributes.class),
                    any(PrintAttributes.class), any(CancellationSignal.class),
                    any(LayoutResultCallback.class), any(Bundle.class));
        }
        if (writeAnswer != null) {
            doAnswer(writeAnswer).when(adapter).onWrite(any(PageRange[].class),
                    any(ParcelFileDescriptor.class), any(CancellationSignal.class),
                    any(WriteResultCallback.class));
        }
        if (finishAnswer != null) {
            doAnswer(finishAnswer).when(adapter).onFinish();
        }
        return adapter;
    }

    static class SimpleTwoPrintersService extends StubPrintService {
        @Override
        public PrinterDiscoverySession onCreatePrinterDiscoverySession() {
            return new PrinterDiscoverySession() {
                @Override
                public void onStartPrinterDiscovery(List<PrinterId> priorityList) {
                    if (getPrinters().isEmpty()) {
                        List<PrinterInfo> printers = new ArrayList<PrinterInfo>();

                        // Add the first printer.
                        PrinterId firstPrinterId = getHost().generatePrinterId("first_printer");
                        PrinterCapabilitiesInfo firstCapabilities =
                                new PrinterCapabilitiesInfo.Builder(firstPrinterId)
                            .setMinMargins(new Margins(200, 200, 200, 200))
                            .addMediaSize(MediaSize.ISO_A4, true)
                            .addMediaSize(MediaSize.ISO_A5, false)
                            .addResolution(new Resolution("300x300", "300x300", 300, 300), true)
                            .setColorModes(PrintAttributes.COLOR_MODE_COLOR,
                                    PrintAttributes.COLOR_MODE_COLOR)
                            .build();
                        PrinterInfo firstPrinter = new PrinterInfo.Builder(firstPrinterId,
                                "First printer", PrinterInfo.STATUS_IDLE)
                            .setCapabilities(firstCapabilities)
                            .build();
                        printers.add(firstPrinter);

                        // Add the second printer.
                        PrinterId secondPrinterId = getHost().generatePrinterId("second_printer");
                        PrinterCapabilitiesInfo secondCapabilities =
                                new PrinterCapabilitiesInfo.Builder(secondPrinterId)
                            .addMediaSize(MediaSize.ISO_A3, true)
                            .addMediaSize(MediaSize.ISO_A4, false)
                            .addResolution(new Resolution("200x200", "200x200", 200, 200), true)
                            .addResolution(new Resolution("300x300", "300x300", 300, 300), false)
                            .setColorModes(PrintAttributes.COLOR_MODE_COLOR
                                    | PrintAttributes.COLOR_MODE_MONOCHROME,
                                    PrintAttributes.COLOR_MODE_MONOCHROME)
                            .build();
                        PrinterInfo secondPrinter = new PrinterInfo.Builder(secondPrinterId,
                                "Second printer", PrinterInfo.STATUS_IDLE)
                            .setCapabilities(secondCapabilities)
                            .build();
                        printers.add(secondPrinter);

                        // Add the third printer.
                        PrinterId thirdPrinterId = getHost().generatePrinterId("third_printer");
                        PrinterCapabilitiesInfo thirdCapabilities =
                                new PrinterCapabilitiesInfo.Builder(thirdPrinterId)
                            .addMediaSize(MediaSize.NA_LETTER, true)
                            .addResolution(new Resolution("300x300", "300x300", 300, 300), true)
                            .setColorModes(PrintAttributes.COLOR_MODE_COLOR,
                                    PrintAttributes.COLOR_MODE_COLOR)
                            .build();
                        PrinterInfo thirdPrinter = new PrinterInfo.Builder(thirdPrinterId,
                                "Third printer", PrinterInfo.STATUS_IDLE)
                            .setCapabilities(thirdCapabilities)
                            .build();
                        printers.add(thirdPrinter);

                        addPrinters(printers);
                    }
                }

                @Override
                public void onStopPrinterDiscovery() {
                    /* do nothing */
                }

                @Override
                public void onValidatePrinters(List<PrinterId> printerIds) {
                    /* do nothing */
                }

                @Override
                public void onStartPrinterStateTracking(PrinterId printerId) {
                    /* do nothing */
                }

                @Override
                public void onStopPrinterStateTracking(PrinterId printerId) {
                    /* do nothing */
                }

                @Override
                public void onDestroy() {
                    /* do nothing */
                }
            };
        }

        @Override
        public void onRequestCancelPrintJob(PrintJob printJob) {
            /* do nothing */
        }

        @Override
        public void onPrintJobQueued(PrintJob printJob) {
            /* do nothing */
        }
    }

    private void verifyLayoutCall(InOrder inOrder, PrintDocumentAdapter mock,
            PrintAttributes oldAttributes, PrintAttributes newAttributes,
            final boolean forPreview) {
        inOrder.verify(mock).onLayout(eq(oldAttributes), eq(newAttributes),
                any(CancellationSignal.class), any(LayoutResultCallback.class), argThat(
                        new BaseMatcher<Bundle>() {
                            @Override
                            public boolean matches(Object item) {
                                Bundle bundle = (Bundle) item;
                                return forPreview == bundle.getBoolean(
                                        PrintDocumentAdapter.EXTRA_PRINT_PREVIEW);
                            }

                            @Override
                            public void describeTo(Description description) {
                                /* do nothing */
                            }
                        }));
    }

    private final class CallCounter {
        private final Object mLock = new Object();

        private int mCallCount;

        public void call() {
            synchronized (mLock) {
                mCallCount++;
            }
        }

        public void waitForCount(int count, long timeoutMIllis) throws TimeoutException {
            synchronized (mLock) {
                final long startTimeMillis = SystemClock.uptimeMillis();
                while (mCallCount < count) {
                    try {
                        final long elapsedTimeMillis = SystemClock.uptimeMillis() - startTimeMillis;
                        final long remainingTimeMillis = timeoutMIllis - elapsedTimeMillis;
                        if (remainingTimeMillis <= 0) {
                            throw new TimeoutException();
                        }
                        mLock.wait(timeoutMIllis);
                    } catch (InterruptedException ie) {
                        /* ignore */
                    }
                }
            }
        }
    }
}
