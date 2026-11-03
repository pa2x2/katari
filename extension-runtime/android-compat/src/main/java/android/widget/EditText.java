/*
 * Copyright (C) 2006 The Android Open Source Project
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

// Modified for Katari's desktop extension host from the Android Open Source Project's EditText: the editable text
// and selection a source's settings configure, kept in a plain buffer without spans.

package android.widget;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.util.AttributeSet;

import java.lang.reflect.Array;

public class EditText extends TextView {
    public EditText(Context context) {
        this(context, null);
    }

    public EditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditText(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public EditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setText("", BufferType.EDITABLE);
    }

    @Override
    protected boolean getDefaultEditable() {
        return true;
    }

    @Override
    public Editable getText() {
        return (Editable) super.getText();
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, BufferType.EDITABLE);
    }

    @Override
    CharSequence createText(CharSequence text, BufferType type) {
        return new TextBuffer(text);
    }

    @Override
    void afterTextChanged() {
        sendAfterTextChanged(getText());
    }

    public void setSelection(int start, int stop) {
        checkSelection(start);
        checkSelection(stop);
        mSelectionStart = start;
        mSelectionEnd = stop;
    }

    public void setSelection(int index) {
        setSelection(index, index);
    }

    public void selectAll() {
        setSelection(0, length());
    }

    public void extendSelection(int index) {
        checkSelection(index);
        mSelectionEnd = index;
    }

    // As AOSP's selection spans refuse a position past the text.
    private void checkSelection(int index) {
        if (index < 0 || index > length()) {
            throw new IndexOutOfBoundsException("setSpan (" + index + " ... " + index + ") ends beyond length " + length());
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return EditText.class.getName();
    }

    /** The text being edited. Spans aren't kept: nothing on desktop draws them. */
    private static final class TextBuffer implements Editable {
        private final StringBuilder mText;
        private InputFilter[] mFilters = new InputFilter[0];

        TextBuffer(CharSequence text) {
            mText = new StringBuilder(text);
        }

        @Override
        public Editable replace(int st, int en, CharSequence source, int start, int end) {
            mText.replace(st, en, source.subSequence(start, end).toString());
            return this;
        }

        @Override
        public Editable replace(int st, int en, CharSequence text) {
            return replace(st, en, text, 0, text.length());
        }

        @Override
        public Editable insert(int where, CharSequence text, int start, int end) {
            return replace(where, where, text, start, end);
        }

        @Override
        public Editable insert(int where, CharSequence text) {
            return replace(where, where, text, 0, text.length());
        }

        @Override
        public Editable delete(int st, int en) {
            return replace(st, en, "", 0, 0);
        }

        @Override
        public Editable append(CharSequence text) {
            return replace(length(), length(), text, 0, text.length());
        }

        @Override
        public Editable append(CharSequence text, int start, int end) {
            return replace(length(), length(), text, start, end);
        }

        @Override
        public Editable append(char text) {
            return append(String.valueOf(text));
        }

        @Override
        public void clear() {
            mText.setLength(0);
        }

        @Override
        public void clearSpans() {}

        @Override
        public void setFilters(InputFilter[] filters) {
            if (filters == null) {
                throw new IllegalArgumentException();
            }
            mFilters = filters;
        }

        @Override
        public InputFilter[] getFilters() {
            return mFilters;
        }

        @Override
        public void setSpan(Object what, int start, int end, int flags) {}

        @Override
        public void removeSpan(Object what) {}

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] getSpans(int start, int end, Class<T> type) {
            return (T[]) Array.newInstance(type, 0);
        }

        @Override
        public int getSpanStart(Object tag) {
            return -1;
        }

        @Override
        public int getSpanEnd(Object tag) {
            return -1;
        }

        @Override
        public int getSpanFlags(Object tag) {
            return 0;
        }

        @Override
        public int nextSpanTransition(int start, int limit, Class type) {
            return limit;
        }

        @Override
        public void getChars(int start, int end, char[] dest, int destoff) {
            mText.getChars(start, end, dest, destoff);
        }

        @Override
        public int length() {
            return mText.length();
        }

        @Override
        public char charAt(int index) {
            return mText.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return mText.substring(start, end);
        }

        @Override
        public String toString() {
            return mText.toString();
        }
    }
}
