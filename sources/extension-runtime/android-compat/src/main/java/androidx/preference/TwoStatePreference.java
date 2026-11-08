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

public abstract class TwoStatePreference extends Preference {
    protected boolean mChecked;
    private CharSequence mSummaryOn;
    private CharSequence mSummaryOff;
    private boolean mCheckedSet;
    private boolean mDisableDependentsState;

    public TwoStatePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public TwoStatePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TwoStatePreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TwoStatePreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onClick() {
        super.onClick();
        final boolean newValue = !isChecked();
        if (callChangeListener(newValue)) {
            setChecked(newValue);
        }
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void setChecked(boolean checked) {
        final boolean changed = mChecked != checked;
        if (changed || !mCheckedSet) {
            mChecked = checked;
            mCheckedSet = true;
            persistBoolean(checked);
            if (changed) {
                notifyDependencyChange(shouldDisableDependents());
                notifyChanged();
            }
        }
    }

    @Override
    public boolean shouldDisableDependents() {
        boolean shouldDisable = mDisableDependentsState ? mChecked : !mChecked;
        return shouldDisable || super.shouldDisableDependents();
    }

    public void setSummaryOn(CharSequence summary) {
        mSummaryOn = summary;
        if (isChecked()) {
            notifyChanged();
        }
    }

    public CharSequence getSummaryOn() {
        return mSummaryOn;
    }

    public void setSummaryOn(int summaryResId) {
        setSummaryOn(getContext().getString(summaryResId));
    }

    public void setSummaryOff(CharSequence summary) {
        mSummaryOff = summary;
        if (!isChecked()) {
            notifyChanged();
        }
    }

    public CharSequence getSummaryOff() {
        return mSummaryOff;
    }

    public void setSummaryOff(int summaryResId) {
        setSummaryOff(getContext().getString(summaryResId));
    }

    public boolean getDisableDependentsState() {
        return mDisableDependentsState;
    }

    public void setDisableDependentsState(boolean disableDependentsState) {
        mDisableDependentsState = disableDependentsState;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getBoolean(index, false);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        if (defaultValue == null) {
            defaultValue = false;
        }
        setChecked(getPersistedBoolean((Boolean) defaultValue));
    }
}
