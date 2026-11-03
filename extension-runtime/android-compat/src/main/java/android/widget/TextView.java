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

// Modified for Katari's desktop extension host from the Android Open Source Project's TextView: what a source's
// settings configure an editor with (text, hint, input type and the single-line rule tied to it, IME options, lines,
// horizontal scrolling, error, filters, watchers and listeners, selection), without layout, drawing, resources or
// input handling.

package android.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import java.util.ArrayList;

public class TextView extends View {
    private CharSequence mText = "";
    private CharSequence mHint;
    private int mInputType = EditorInfo.TYPE_NULL;
    private int mImeOptions = EditorInfo.IME_NULL;
    private boolean mSingleLine;
    private int mMaxLines = Integer.MAX_VALUE;
    private int mMinLines = 0;
    private boolean mHorizontallyScrolling;
    private CharSequence mError;
    private InputFilter[] mFilters = new InputFilter[0];
    private final ArrayList<TextWatcher> mListeners = new ArrayList<>();
    private OnEditorActionListener mOnEditorActionListener;
    int mSelectionStart = -1;
    int mSelectionEnd = -1;

    public TextView(Context context) {
        this(context, null);
    }

    public TextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (getDefaultEditable()) {
            // As AOSP's constructor sets up an editable view without an input type in its attributes.
            mInputType = EditorInfo.TYPE_CLASS_TEXT;
            setInputTypeSingleLine(false);
        }
    }

    protected boolean getDefaultEditable() {
        return false;
    }

    public final void setText(CharSequence text) {
        setText(text, BufferType.NORMAL);
    }

    public void setText(CharSequence text, BufferType type) {
        if (text == null) {
            text = "";
        }
        final CharSequence old = mText;
        for (TextWatcher watcher : new ArrayList<>(mListeners)) {
            watcher.beforeTextChanged(old, 0, old.length(), text.length());
        }
        mText = createText(text, type);
        if (mSelectionStart > mText.length() || mSelectionEnd > mText.length()) {
            mSelectionStart = mText.length();
            mSelectionEnd = mText.length();
        }
        for (TextWatcher watcher : new ArrayList<>(mListeners)) {
            watcher.onTextChanged(mText, 0, old.length(), mText.length());
        }
        afterTextChanged();
    }

    public final void setText(char[] text, int start, int len) {
        setText(new String(text, start, len));
    }

    /** What {@link #setText(CharSequence, BufferType)} keeps; an editable view keeps its own buffer. */
    CharSequence createText(CharSequence text, BufferType type) {
        return text.toString();
    }

    void afterTextChanged() {}

    void sendAfterTextChanged(android.text.Editable text) {
        for (TextWatcher watcher : new ArrayList<>(mListeners)) {
            watcher.afterTextChanged(text);
        }
    }

    public CharSequence getText() {
        return mText;
    }

    public int length() {
        return mText.length();
    }

    public final void setHint(CharSequence hint) {
        mHint = hint;
    }

    public CharSequence getHint() {
        return mHint;
    }

    public void setInputType(int type) {
        mInputType = type;
        final boolean singleLine = !isMultilineInputType(type);
        if (mSingleLine != singleLine) {
            applySingleLine(singleLine);
        }
    }

    public void setRawInputType(int type) {
        mInputType = type;
    }

    public int getInputType() {
        return mInputType;
    }

    public void setImeOptions(int imeOptions) {
        mImeOptions = imeOptions;
    }

    public int getImeOptions() {
        return mImeOptions;
    }

    public void setSingleLine() {
        setSingleLine(true);
    }

    public void setSingleLine(boolean singleLine) {
        setInputTypeSingleLine(singleLine);
        applySingleLine(singleLine);
    }

    private void applySingleLine(boolean singleLine) {
        mSingleLine = singleLine;
        if (singleLine) {
            setLines(1);
        } else {
            setMaxLines(Integer.MAX_VALUE);
        }
    }

    public boolean isSingleLine() {
        return mSingleLine;
    }

    public void setMaxLines(int maxLines) {
        mMaxLines = maxLines;
    }

    public int getMaxLines() {
        return mMaxLines;
    }

    public void setMinLines(int minLines) {
        mMinLines = minLines;
    }

    public int getMinLines() {
        return mMinLines;
    }

    public void setLines(int lines) {
        mMaxLines = lines;
        mMinLines = lines;
    }

    public void setHorizontallyScrolling(boolean whether) {
        mHorizontallyScrolling = whether;
    }

    public final boolean isHorizontallyScrollable() {
        return mHorizontallyScrolling;
    }

    public void setError(CharSequence error) {
        mError = error;
    }

    public void setError(CharSequence error, Drawable icon) {
        mError = error;
    }

    public CharSequence getError() {
        return mError;
    }

    public void setFilters(InputFilter[] filters) {
        if (filters == null) {
            throw new IllegalArgumentException();
        }
        mFilters = filters;
    }

    public InputFilter[] getFilters() {
        return mFilters;
    }

    public void addTextChangedListener(TextWatcher watcher) {
        mListeners.add(watcher);
    }

    public void removeTextChangedListener(TextWatcher watcher) {
        mListeners.remove(watcher);
    }

    public void setOnEditorActionListener(OnEditorActionListener listener) {
        mOnEditorActionListener = listener;
    }

    public int getSelectionStart() {
        return mSelectionStart;
    }

    public int getSelectionEnd() {
        return mSelectionEnd;
    }

    public boolean hasSelection() {
        return mSelectionStart >= 0 && mSelectionStart != mSelectionEnd;
    }

    // AOSP keeps an editable class-text input's multi-line flag in step with the single-line setting.
    private void setInputTypeSingleLine(boolean singleLine) {
        if ((mInputType & EditorInfo.TYPE_MASK_CLASS) == EditorInfo.TYPE_CLASS_TEXT && getDefaultEditable()) {
            if (singleLine) {
                mInputType &= ~EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
            } else {
                mInputType |= EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
            }
        }
    }

    private static boolean isMultilineInputType(int type) {
        return (type & (InputType.TYPE_MASK_CLASS | InputType.TYPE_TEXT_FLAG_MULTI_LINE))
                == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    }

    public enum BufferType {
        NORMAL, SPANNABLE, EDITABLE
    }

    public interface OnEditorActionListener {
        boolean onEditorAction(TextView v, int actionId, KeyEvent event);
    }
}
