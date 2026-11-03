/*
 * Copyright (C) 2012 The Android Open Source Project
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

// Modified for Katari's desktop extension host from AndroidX Core's ContextCompat, which Katari's Android app ships
// and so extensions call without bundling it: the members extensions use. The main executor is the one AndroidX makes
// before API 28, over a handler on the context's main looper, since the stand-ins' Context predates getMainExecutor.

package androidx.core.content;

import android.content.Context;
import android.os.Handler;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public class ContextCompat {
    protected ContextCompat() {
    }

    public static Executor getMainExecutor(Context context) {
        Handler handler = new Handler(context.getMainLooper());
        return command -> {
            if (!handler.post(command)) {
                throw new RejectedExecutionException(handler + " is shutting down");
            }
        };
    }
}
