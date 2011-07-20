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

import com.android.cts.stub.R;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Custom view group to handle density changes while maintaining screen independence.
 */
public class TesterViewGroup extends ViewGroup {
    private int mReferenceWidthDp;
    private int mReferenceHeightDp;

    public TesterViewGroup(Context context) {
        this(context, null);
    }

    public TesterViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources resources = context.getResources();

        mReferenceWidthDp = resources.getDimensionPixelSize(R.dimen.reference_width);
        mReferenceHeightDp = resources.getDimensionPixelSize(R.dimen.reference_height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (!changed) {
            return;
        }

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        widthMeasureSpec = getMeasureSpec(LayoutParams.MATCH_PARENT, mReferenceWidthDp);
        heightMeasureSpec = getMeasureSpec(LayoutParams.MATCH_PARENT, mReferenceHeightDp);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);

            LayoutParams params = child.getLayoutParams();

            int height = getMeasureSpec(params.height, mReferenceWidthDp);
            int width = getMeasureSpec(params.width, mReferenceHeightDp);
            child.measure(width, height);
        }
    }

    private int getMeasureSpec(int val, int referenceSize) {
        if (val == LayoutParams.MATCH_PARENT) {
            return MeasureSpec.makeMeasureSpec(referenceSize, MeasureSpec.EXACTLY);
        } else if (val == LayoutParams.WRAP_CONTENT) {
            return MeasureSpec.makeMeasureSpec(referenceSize, MeasureSpec.AT_MOST);
        } else {
            return val;
        }
    }
}
