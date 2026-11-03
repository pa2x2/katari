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

// Katari's model of the Android Open Source Project's hidden ActivityThread, limited to what extensions read of it by
// reflection to find the foreground activity, as the application's lifecycle callbacks miss an activity resumed before
// they were registered: currentActivityThread() and its mActivities, whose records hold the activity and whether it is
// paused. The host, standing for the system, launches and destroys the activity on the main looper's thread
// (launchActivity and destroyActivity, Katari's). Records are keyed by their activity, where AOSP's are keyed by its
// binder token.

package android.app;

import android.util.ArrayMap;

public final class ActivityThread {
    private static final ActivityThread sCurrentActivityThread = new ActivityThread();

    final ArrayMap<Object, ActivityClientRecord> mActivities = new ArrayMap<>();

    private ActivityThread() {
    }

    public static ActivityThread currentActivityThread() {
        return sCurrentActivityThread;
    }

    public static final class ActivityClientRecord {
        Activity activity;
        boolean paused;
        boolean stopped;
    }

    /** Katari's: creates, starts and resumes an activity of {@code application}, on the main looper's thread. */
    public Activity launchActivity(Application application) {
        Activity activity = new Activity();
        activity.attach(application.getBaseContext(), application);
        ActivityClientRecord record = new ActivityClientRecord();
        record.activity = activity;
        record.paused = true;
        record.stopped = true;
        mActivities.put(activity, record);
        activity.onCreate(null);
        activity.onStart();
        record.stopped = false;
        activity.onResume();
        record.paused = false;
        return activity;
    }

    /** Katari's: finishes, pauses, stops and destroys {@code activity}, on the main looper's thread. */
    public void destroyActivity(Activity activity) {
        ActivityClientRecord record = mActivities.get(activity);
        if (record == null) return;
        activity.performFinish();
        activity.onPause();
        record.paused = true;
        activity.onStop();
        record.stopped = true;
        mActivities.remove(activity);
        activity.performDestroy();
    }
}
