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

// Katari's model of the Android Open Source Project's Activity, limited to what extensions use of the activity they
// find in the foreground: on desktop the one activity there is stands for the app's window, which ActivityThread
// launches as the window opens and finishes as it closes. Its lifecycle reaches the application's lifecycle callbacks
// as AOSP's does, and a Dialog shown over it opens in the window. It has no window, views, intents or results of its
// own; finality and access of its members are AOSP's, so extensions' own activity classes still load.

package android.app;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextThemeWrapper;

public class Activity extends ContextThemeWrapper {
    private Application mApplication;
    private Thread mUiThread;
    private Handler mHandler;
    private boolean mFinished;
    private boolean mDestroyed;

    public Activity() {
    }

    // Context and application are AOSP's ContextImpl's and Application; the activity runs on the calling thread
    final void attach(Context context, Application application) {
        attachBaseContext(context);
        mApplication = application;
        mUiThread = Thread.currentThread();
        mHandler = new Handler(Looper.myLooper());
    }

    public final Application getApplication() {
        return mApplication;
    }

    // An activity's context hands its application, as AOSP's ContextImpl does
    @Override
    public Context getApplicationContext() {
        return mApplication;
    }

    public boolean isFinishing() {
        return mFinished;
    }

    public boolean isDestroyed() {
        return mDestroyed;
    }

    public final void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != mUiThread) {
            mHandler.post(action);
        } else {
            action.run();
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        mApplication.dispatchActivityCreated(this, savedInstanceState);
    }

    protected void onStart() {
        mApplication.dispatchActivityStarted(this);
    }

    protected void onResume() {
        mApplication.dispatchActivityResumed(this);
    }

    protected void onPause() {
        mApplication.dispatchActivityPaused(this);
    }

    protected void onStop() {
        mApplication.dispatchActivityStopped(this);
    }

    protected void onDestroy() {
        mApplication.dispatchActivityDestroyed(this);
    }

    // AOSP's ActivityThread finishes an activity before destroying it
    final void performFinish() {
        mFinished = true;
    }

    final void performDestroy() {
        mDestroyed = true;
        onDestroy();
    }
}
