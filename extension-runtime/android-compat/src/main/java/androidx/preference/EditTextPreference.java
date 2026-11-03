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
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.EditText;

public class EditTextPreference extends DialogPreference {
    private String mText;
    private OnBindEditTextListener mOnBindEditTextListener;

    public EditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public EditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public EditTextPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditTextPreference(Context context) {
        this(context, null);
    }

    public void setText(String text) {
        final boolean wasBlocking = shouldDisableDependents();
        mText = text;
        persistString(text);
        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
        notifyChanged();
    }

    public String getText() {
        return mText;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        setText(getPersistedString((String) defaultValue));
    }

    @Override
    public boolean shouldDisableDependents() {
        return TextUtils.isEmpty(mText) || super.shouldDisableDependents();
    }

    public void setOnBindEditTextListener(OnBindEditTextListener onBindEditTextListener) {
        mOnBindEditTextListener = onBindEditTextListener;
    }

    OnBindEditTextListener getOnBindEditTextListener() {
        return mOnBindEditTextListener;
    }

    public interface OnBindEditTextListener {
        void onBindEditText(EditText editText);
    }

    public static final class SimpleSummaryProvider implements SummaryProvider<EditTextPreference> {
        private static SimpleSummaryProvider sSimpleSummaryProvider;

        private SimpleSummaryProvider() {}

        public static SimpleSummaryProvider getInstance() {
            if (sSimpleSummaryProvider == null) {
                sSimpleSummaryProvider = new SimpleSummaryProvider();
            }
            return sSimpleSummaryProvider;
        }

        // AndroidX's own English text; desktop has no AndroidX string resources.
        @Override
        public CharSequence provideSummary(EditTextPreference preference) {
            if (TextUtils.isEmpty(preference.getText())) {
                return "Not set";
            } else {
                return preference.getText();
            }
        }
    }
}
