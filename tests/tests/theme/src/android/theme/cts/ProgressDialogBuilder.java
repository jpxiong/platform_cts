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

package android.theme.cts;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.view.View;

/**
 * This class implements the {@link DialogBuilder} interface
 * in order to build {@link ProgressDialog}s for testing purposes.
 *
 * <p>In order to add more tests of {@link ProgressDialog}, simply
 * add another case inside {@link ProgressDialogBuilder#buildDialog}.
 */
public class ProgressDialogBuilder implements DialogBuilder {
    /**
     * Builds a {@link ProgressDialog} that contains a circular
     * {@link ProgressBar}.
     */
    public static final int SPINNER = 0;

    /**
     * Builds a {@link ProgressDialog} that contains a horizontal
     * {@link ProgressBar}.
     */
    public static final int HORIZONTAL = 1;

    private int mDialogType;

    public ProgressDialogBuilder(int dialogType) {
        mDialogType = dialogType;
    }

    @Override
    public Dialog buildDialog(View view) {
        ProgressDialog progressDialog = new ProgressDialog(view.getContext());
        progressDialog.setMessage("Loading...");

        switch (mDialogType) {
            case SPINNER:
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                break;
            case HORIZONTAL:
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMax(100);
                break;
        }

        progressDialog.show();
        return progressDialog;
    }

}
