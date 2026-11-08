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

// Katari's model of the Android Open Source Project's Dialog, limited to what extensions use to show a web view over
// the foreground activity: a dialog over an activity whose content is a WebView opens in the app's window
// (app.katari.extension.compat.ExtensionDialogs, which says how it looks), showing that web view's page. A dialog with any
// other content throws as it is shown, as does one over a context that has no running activity, as Android throws
// BadTokenException. The dialog has no window of its own, so getWindow() is null, which the SDK allows, and what
// extensions set on a window is never asked for. Lifecycle, listeners and their threads are AOSP's: the user closing
// the dialog presses back, which cancels a cancelable one, and listeners run on the thread the dialog was made on.

package android.app;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;

import app.katari.extension.compat.AndroidCompatEnvironment;
import app.katari.extension.compat.ShownDialog;

public class Dialog implements DialogInterface {
    private final Context mContext;
    private final Handler mHandler = new Handler(Looper.myLooper());
    private Activity mOwnerActivity;

    private View mContentView;
    private CharSequence mTitle;
    private boolean mCancelable = true;
    private boolean mCanceledOnTouchOutside;

    private OnCancelListener mOnCancelListener;
    private OnDismissListener mOnDismissListener;
    private OnShowListener mOnShowListener;

    private boolean mCreated;
    private boolean mShowing;
    private boolean mCanceled;
    private ShownDialog mShown;

    public Dialog(@NonNull Context context) {
        this(context, 0);
    }

    public Dialog(@NonNull Context context, int themeResId) {
        mContext = new ContextThemeWrapper(context, themeResId);
        if (context instanceof Activity) {
            mOwnerActivity = (Activity) context;
        }
    }

    protected Dialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        this(context);
        mCancelable = cancelable;
        mOnCancelListener = cancelListener;
    }

    @NonNull
    public final Context getContext() {
        return mContext;
    }

    public final void setOwnerActivity(@NonNull Activity activity) {
        mOwnerActivity = activity;
    }

    @Nullable
    public final Activity getOwnerActivity() {
        return mOwnerActivity;
    }

    @Nullable
    public Window getWindow() {
        return null;
    }

    public boolean isShowing() {
        return mShowing;
    }

    public void setContentView(int layoutResID) {
        throw new UnsupportedOperationException(
                "Desktop shows only a dialog whose content is a web view, not a layout resource");
    }

    public void setContentView(@NonNull View view) {
        mContentView = view;
    }

    public void setContentView(@NonNull View view, @Nullable ViewGroup.LayoutParams params) {
        mContentView = view;
    }

    public void setTitle(@Nullable CharSequence title) {
        mTitle = title;
        updateShown();
    }

    public void setTitle(int titleId) {
        setTitle(mContext.getText(titleId));
    }

    public void setCancelable(boolean flag) {
        mCancelable = flag;
        updateShown();
    }

    public void setCanceledOnTouchOutside(boolean cancel) {
        if (cancel && !mCancelable) {
            mCancelable = true;
        }
        mCanceledOnTouchOutside = cancel;
        updateShown();
    }

    public void setOnCancelListener(@Nullable OnCancelListener listener) {
        mOnCancelListener = listener;
    }

    public void setOnDismissListener(@Nullable OnDismissListener listener) {
        mOnDismissListener = listener;
    }

    public void setOnShowListener(@Nullable OnShowListener listener) {
        mOnShowListener = listener;
    }

    protected void onCreate(Bundle savedInstanceState) {
    }

    protected void onStart() {
    }

    protected void onStop() {
    }

    public void create() {
        if (!mCreated) {
            mCreated = true;
            onCreate(null);
        }
    }

    public void show() {
        if (mShowing) {
            return;
        }
        mCanceled = false;
        create();
        onStart();
        Activity activity = runningActivity();
        if (activity == null) {
            throw new IllegalStateException(
                    "Unable to add window -- token null is not valid; is your activity running?");
        }
        if (!(mContentView instanceof WebView)) {
            throw new UnsupportedOperationException("Desktop shows only a dialog whose content is a web view, not "
                    + (mContentView == null ? "no content" : "a " + mContentView.getClass().getName()));
        }
        mShown = AndroidCompatEnvironment.current().getDialogs().show(
                ((WebView) mContentView).getHostPage(),
                mTitle,
                mCancelable,
                mCanceledOnTouchOutside,
                () -> mHandler.post(this::onUserClose));
        mShowing = true;
        OnShowListener listener = mOnShowListener;
        if (listener != null) {
            mHandler.post(() -> listener.onShow(this));
        }
    }

    @Override
    public void dismiss() {
        if (Looper.myLooper() == mHandler.getLooper()) {
            dismissDialog();
        } else {
            mHandler.post(this::dismissDialog);
        }
    }

    @Override
    public void cancel() {
        OnCancelListener listener = mOnCancelListener;
        if (!mCanceled && listener != null) {
            mCanceled = true;
            mHandler.post(() -> listener.onCancel(this));
        }
        dismiss();
    }

    public void onBackPressed() {
        if (mCancelable) {
            cancel();
        }
    }

    private void onUserClose() {
        if (mShowing) {
            onBackPressed();
        }
    }

    private void dismissDialog() {
        if (!mShowing) {
            return;
        }
        mShowing = false;
        mShown.dismiss();
        mShown = null;
        onStop();
        OnDismissListener listener = mOnDismissListener;
        if (listener != null) {
            mHandler.post(() -> listener.onDismiss(this));
        }
    }

    private void updateShown() {
        if (mShown != null) {
            mShown.update(mTitle, mCancelable, mCanceledOnTouchOutside);
        }
    }

    // The activity whose window the dialog's context belongs to, as Android finds its window token
    @Nullable
    private Activity runningActivity() {
        Context context = mContext;
        while (context instanceof ContextWrapper && !(context instanceof Activity)) {
            context = ((ContextWrapper) context).getBaseContext();
        }
        if (!(context instanceof Activity)) {
            return null;
        }
        Activity activity = (Activity) context;
        return activity.isFinishing() || activity.isDestroyed() ? null : activity;
    }
}
