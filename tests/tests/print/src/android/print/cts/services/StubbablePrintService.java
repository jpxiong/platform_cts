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

package android.print.cts.services;

import android.print.PrinterId;
import android.printservice.PrintJob;
import android.printservice.PrintService;
import android.printservice.PrinterDiscoverySession;

import java.util.List;

public abstract class StubbablePrintService extends BasePrintService {

    @Override
    public PrinterDiscoverySession onCreatePrinterDiscoverySession() {
        BasePrintService impl = getStub(this);
        if (impl != null) {
            return impl.onCreatePrinterDiscoverySession();
        }
        return new StubSession();
    }

    @Override
    public void onRequestCancelPrintJob(PrintJob printJob) {
        BasePrintService impl = getStub(this);
        if (impl != null) {
            impl.onRequestCancelPrintJob(printJob);
        }
    }

    @Override
    public void onPrintJobQueued(PrintJob printJob) {
        BasePrintService impl = getStub(this);
        if (impl != null) {
            impl.onPrintJobQueued(printJob);
        }
    }

    protected abstract BasePrintService getStub(PrintService host);

    private final class StubSession extends PrinterDiscoverySession {
        @Override
        public void onValidatePrinters(List<PrinterId> printerIds) {
            /* do nothing */
        }

        @Override
        public void onStopPrinterStateTracking(PrinterId printerId) {
            /* do nothing */
        }

        @Override
        public void onStopPrinterDiscovery() {
            /* do nothing */
        }

        @Override
        public void onStartPrinterStateTracking(PrinterId printerId) {
            /* do nothing */
        }

        @Override
        public void onStartPrinterDiscovery(List<PrinterId> priorityList) {
            /* do nothing */
        }

        @Override
        public void onDestroy() {
            /* do nothing */
        }
    }
}
