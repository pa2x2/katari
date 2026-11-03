/*
 * Copyright 2018 The Android Open Source Project
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

// Modified for Katari's desktop extension host from AndroidX Preference 1.2.1: the preference model only, without
// views, XML inflation, styled attributes or saved instance state.

package androidx.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

public class SeekBarPreference extends Preference {
    int mSeekBarValue;
    int mMin;
    private int mMax;
    private int mSeekBarIncrement;
    boolean mAdjustable;
    private boolean mShowSeekBarValue;
    boolean mUpdatesContinuously;

    // AndroidX's default style: 0 to 100, the seek bar's own increment, adjustable, value hidden.
    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mMin = 0;
        setMax(100);
        setSeekBarIncrement(0);
        mAdjustable = true;
        mShowSeekBarValue = false;
        mUpdatesContinuously = false;
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeekBarPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        if (defaultValue == null) {
            defaultValue = 0;
        }
        setValue(getPersistedInt((Integer) defaultValue));
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    public int getMin() {
        return mMin;
    }

    public void setMin(int min) {
        if (min > mMax) {
            min = mMax;
        }
        if (min != mMin) {
            mMin = min;
            notifyChanged();
        }
    }

    public final int getSeekBarIncrement() {
        return mSeekBarIncrement;
    }

    public final void setSeekBarIncrement(int seekBarIncrement) {
        if (seekBarIncrement != mSeekBarIncrement) {
            mSeekBarIncrement = Math.min(mMax - mMin, Math.abs(seekBarIncrement));
            notifyChanged();
        }
    }

    public int getMax() {
        return mMax;
    }

    public final void setMax(int max) {
        if (max < mMin) {
            max = mMin;
        }
        if (max != mMax) {
            mMax = max;
            notifyChanged();
        }
    }

    public boolean isAdjustable() {
        return mAdjustable;
    }

    public void setAdjustable(boolean adjustable) {
        mAdjustable = adjustable;
    }

    public boolean getUpdatesContinuously() {
        return mUpdatesContinuously;
    }

    public void setUpdatesContinuously(boolean updatesContinuously) {
        mUpdatesContinuously = updatesContinuously;
    }

    public boolean getShowSeekBarValue() {
        return mShowSeekBarValue;
    }

    public void setShowSeekBarValue(boolean showSeekBarValue) {
        mShowSeekBarValue = showSeekBarValue;
        notifyChanged();
    }

    private void setValueInternal(int seekBarValue, boolean notifyChanged) {
        if (seekBarValue < mMin) {
            seekBarValue = mMin;
        }
        if (seekBarValue > mMax) {
            seekBarValue = mMax;
        }
        if (seekBarValue != mSeekBarValue) {
            mSeekBarValue = seekBarValue;
            persistInt(seekBarValue);
            if (notifyChanged) {
                notifyChanged();
            }
        }
    }

    public int getValue() {
        return mSeekBarValue;
    }

    public void setValue(int seekBarValue) {
        setValueInternal(seekBarValue, true);
    }
}
